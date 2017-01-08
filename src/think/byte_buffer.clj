(ns think.byte-buffer
  (:require [think.datatype.core :as dtype]
            [think.datatype.marshal :as marshal]
            [clojure.core.matrix.macros :refer [c-for]]
            [clojure.core.matrix.protocols :as mp]
            [think.resource.core :as resource]
            [clojure.core.matrix :as m])
  (:import [think.byte_buffer ByteBuffer
            ByteBuffer$EndianType
            ByteBuffer$Datatype
            ByteBuffer$BufferManager]
           [think.datatype DoubleArrayView FloatArrayView
            LongArrayView IntArrayView ShortArrayView ByteArrayView]
           [java.nio ShortBuffer IntBuffer LongBuffer
            FloatBuffer DoubleBuffer Buffer]
           [org.bytedeco.javacpp DoublePointer FloatPointer]))


(set! *warn-on-reflection* true)


(defn create-manager
  ^ByteBuffer$BufferManager []
  (ByteBuffer$BufferManager/create_buffer_manager))


(defn ->cpp-datatype
  ^long [datatype]
  (condp = datatype
    :byte ByteBuffer$Datatype/Byte
    :short ByteBuffer$Datatype/Short
    :int ByteBuffer$Datatype/Int
    :long ByteBuffer$Datatype/Long
    :float ByteBuffer$Datatype/Float
    :double ByteBuffer$Datatype/Double))


(defprotocol CopyToTypedBuffer
  "Internal protocol to this library; maps the typed buffer operations
onto other datatypes."
  (copy-to-typed-buffer! [src src-offset typed-buffer dest-offset elem-count]))


(defprotocol BufferAccess
  "Internal protocol, maps the set call into the appropriate set_value overload."
  (set-typed-buffer-value! [value typed-buffer offset n-elems]))


(defn check-buffer-access
  [^long size ^long offset ^long elem-count]
  (when-not (<= (+ offset elem-count) size)
    (throw (ex-info "Buffer access violation"
                    {:size size
                     :offset offset
                     :elem-count elem-count}))))




(defrecord TypedBuffer [^long data ^long size datatype
                        ^ByteBuffer$BufferManager manager]
  dtype/PDatatype
  (get-datatype [this] datatype)
  dtype/PAccess
  (set-value! [this offset value]
    (check-buffer-access size offset 1)
    (set-typed-buffer-value! value this offset 1))
  (set-constant! [this offset value n-elems]
    (check-buffer-access size offset n-elems)
    (set-typed-buffer-value! value this offset n-elems))
  (get-value [this offset]
    (check-buffer-access size offset 1)
    (condp = datatype
      :byte (.get_value_int8 manager data (->cpp-datatype datatype) offset)
      :short (.get_value_int16 manager data (->cpp-datatype datatype) offset)
      :int (.get_value_int32 manager data (->cpp-datatype datatype) offset)
      :long (.get_value_int64 manager data (->cpp-datatype datatype) offset)
      :float (.get_value_float manager data (->cpp-datatype datatype) offset)
      :double (.get_value_double manager data (->cpp-datatype datatype) offset)))
  dtype/PCopyQueryDirect
  (get-direct-copy-fn [this dest-offset]
    #(do
       (check-buffer-access size dest-offset %3)
       (copy-to-typed-buffer! %1 %2 this dest-offset %3)))
  dtype/PCopyQueryIndirect
  (get-indirect-copy-fn [this dest-offset]
    #(do
       (check-buffer-access size dest-offset %3)
       (copy-to-typed-buffer! %1 %2 this dest-offset %3)))
  mp/PElementCount
  (element-count [this] size)
  dtype/PCopyToItemDirect
  (copy-to-buffer-direct! [item item-offset dest dest-offset elem-count]
    (check-buffer-access size item-offset elem-count)
    (dtype/generic-copy! item item-offset dest dest-offset elem-count))
  ;;Direct and indirect copies are all identical from this point of view.
  (copy-to-array-direct! [item item-offset dest dest-offset elem-count]
    (check-buffer-access size item-offset elem-count)
    ((dtype/get-indirect-copy-fn dest dest-offset) item item-offset elem-count))
  dtype/PView
  (->view-impl [this offset elem-count]
    (check-buffer-access size offset elem-count)
    (->TypedBuffer (+ data (* offset (dtype/datatype->byte-size datatype)))
                   elem-count datatype manager))
  CopyToTypedBuffer
  (copy-to-typed-buffer! [src src-offset dest dest-offset elem-count]
    (let [^TypedBuffer dest dest]
     (.copy ^ByteBuffer$BufferManager manager
            data (->cpp-datatype datatype) (long src-offset)
            (.data dest) (->cpp-datatype (.datatype dest)) (long dest-offset)
            (long elem-count))))
  resource/PResource
  (release-resource [this]
    (.release-buffer manager data)))


(defn to-typed-buffer
  ^TypedBuffer [obj] obj)


(defmacro typed-buffer->array-impl
  [ary-type ary-type-fn copy-to-fn cast-fn]
  `[(keyword (name ~copy-to-fn))
    (fn [src# src-offset# dest# dest-offset# elem-count#]
      (let [src# (to-typed-buffer src#)]
        (.copy ^ByteBuffer$BufferManager (.manager src#)
               (.data src#) (int (->cpp-datatype (.datatype src#))) (long src-offset#)
               (~ary-type-fn dest#) (long dest-offset#) (long elem-count#))))])


(extend TypedBuffer
  marshal/PCopyToArray
  (->> (marshal/array-type-iterator typed-buffer->array-impl)
       (into {})))


(defmacro typed-buffer-array-binding
  [ary-type ary-type-fn copy-to-fn cast-fn]
  `(extend ~ary-type
     CopyToTypedBuffer
     {:copy-to-typed-buffer! (fn [src# src-offset# dest# dest-offset# elem-count#]
                               (let [dest# (to-typed-buffer dest#)]
                                 (.copy ^ByteBuffer$BufferManager (.manager dest#)
                                        (~ary-type-fn src#) (long src-offset#)
                                        (.data dest#) (int (->cpp-datatype (.datatype dest#))) (long dest-offset#)
                                        (long elem-count#))))}))


(def typed-buffer-array-bindings (marshal/array-type-iterator typed-buffer-array-binding))


(defn- array-view-copy-to-typed-buffer
  [src src-offset dest dest-offset elem-count]
  (copy-to-typed-buffer! (marshal/view->array src)
                         (marshal/view->array-offset src src-offset)
                         dest dest-offset elem-count))


(defmacro typed-buffer-array-view-binding
  [view-type view-type-fn copy-to-fn cast-fn]
  `(extend ~view-type
     CopyToTypedBuffer
     {:copy-to-typed-buffer! array-view-copy-to-typed-buffer}))


(def typed-buffer-array-view-bindings (marshal/array-view-iterator typed-buffer-array-view-binding))


(defmacro set-typed-buffer-value-impl
  [value typed-buffer offset n-elems cast-fn]
  `(.set_value ^ByteBuffer$BufferManager (.manager ~typed-buffer)
               (long (.data ~typed-buffer)) (int (->cpp-datatype (.datatype ~typed-buffer)))
                              (long ~offset) (~cast-fn ~value) (long ~n-elems)))

(extend-protocol BufferAccess
  Byte
  (set-typed-buffer-value! [value typed-buffer offset n-elems]
    (set-typed-buffer-value-impl value ^TypedBuffer typed-buffer offset n-elems byte))
  Short
  (set-typed-buffer-value! [value typed-buffer offset n-elems]
    (set-typed-buffer-value-impl value ^TypedBuffer typed-buffer offset n-elems short))
  Integer
  (set-typed-buffer-value! [value typed-buffer offset n-elems]
    (set-typed-buffer-value-impl value ^TypedBuffer typed-buffer offset n-elems int))
  Long
  (set-typed-buffer-value! [value typed-buffer offset n-elems]
    (set-typed-buffer-value-impl value ^TypedBuffer typed-buffer offset n-elems long))
  Float
  (set-typed-buffer-value! [value typed-buffer offset n-elems]
    (set-typed-buffer-value-impl value ^TypedBuffer typed-buffer offset n-elems float))
  Double
  (set-typed-buffer-value! [value typed-buffer offset n-elems]
    (set-typed-buffer-value-impl value ^TypedBuffer typed-buffer offset n-elems double))
  ;;If object attempt conversion to double.
  Object
  (set-typed-buffer-value! [value typed-buffer offset n-elems]
    (set-typed-buffer-value-impl value ^TypedBuffer typed-buffer offset n-elems double)))


(defonce ^:dynamic *manager* (atom nil))


(defn default-manager
  ^ByteBuffer$BufferManager []
  (when-not @*manager*
    (reset! *manager* (create-manager)))
  @*manager*)


(defn make-typed-buffer
  [datatype size-or-seq]
  (let [manager (default-manager)
        retval
        (if (number? size-or-seq)
          ;;If just a number then we can allocate directly
          ;;data is expected to be zero initialized.
          (let [data-len (long size-or-seq)
                buf-data (.allocate_buffer manager (* data-len
                                                      (dtype/datatype->byte-size datatype))
                                           "byte-buffer.clj" 286)
                retval (->TypedBuffer buf-data data-len datatype manager)]
            (.set_value ^ByteBuffer$BufferManager manager
                        (long buf-data) (int (->cpp-datatype datatype)) (long 0)
                        (byte 0) (long data-len))
            retval)
          (let [src-data (dtype/make-array-of-type datatype size-or-seq)
                data-len (m/ecount src-data)
                buf-data (.allocate_buffer ^ByteBuffer$BufferManager manager
                                           (long (* data-len (dtype/datatype->byte-size datatype)))
                                           "byte-buffer.clj" 295)
                retval (->TypedBuffer buf-data data-len datatype manager)]
            (dtype/copy! src-data 0 retval 0 data-len)
            retval))]
    (resource/track retval)))
