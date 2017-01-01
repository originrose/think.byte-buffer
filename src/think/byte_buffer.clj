(ns think.byte-buffer
  (:require [think.datatype.core :as dtype]
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
            FloatBuffer DoubleBuffer Buffer]))


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


(defprotocol TypedBufferIO
  "Internal protocol to this library; maps the typed buffer operations
onto other datatypes."
  (copy-to-typed-buffer! [src src-offset typed-buffer dest-offset elem-count])
  (copy-from-typed-buffer! [dest dest-offset typed-buffer src-offset elem-count]))


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
  (copy-to-array-direct! [item item-offset dest dest-offset elem-count]
    (check-buffer-access size item-offset elem-count)
    (copy-from-typed-buffer! dest dest-offset item item-offset elem-count))
  dtype/PView
  (->view-impl [this offset elem-count]
    (check-buffer-access size offset elem-count)
    (->TypedBuffer (+ data (* offset (dtype/datatype->byte-size datatype)))
                   elem-count datatype manager))
  TypedBufferIO
  (copy-to-typed-buffer! [src src-offset dest dest-offset elem-count]
    (let [^TypedBuffer dest dest]
     (.copy ^ByteBuffer$BufferManager manager
            data (->cpp-datatype datatype) (long src-offset)
            (.data dest) (->cpp-datatype (.datatype dest)) (long dest-offset)
            (long elem-count))))
  (copy-from-typed-buffer! [dest dest-offset src src-offset elem-count]
    (let [^TypedBuffer src src]
      (.copy ^ByteBuffer$BufferManager manager
             (.data src) (->cpp-datatype (.datatype src)) (long src-offset)
            data (->cpp-datatype datatype) (long dest-offset)
            (long elem-count))))
  resource/PResource
  (release-resource [this]
    (.release-buffer manager data)))


(defmacro array-copy-to-impl
  [src src-offset typed-buffer dest-offset elem-count]
  `(do
     (check-buffer-access (alength ~src) ~src-offset ~elem-count)
     (.copy ^ByteBuffer$BufferManager (.manager ~typed-buffer)
            ~src (long ~src-offset)
            (.data ~typed-buffer) (->cpp-datatype (.datatype ~typed-buffer)) (long ~dest-offset)
            (long ~elem-count))))


(defmacro array-copy-from-impl
  [typed-buffer src-offset dest dest-offset elem-count]
  `(do
     (check-buffer-access (alength ~dest) ~dest-offset ~elem-count)
     (.copy ^ByteBuffer$BufferManager (.manager ~typed-buffer)
            (.data ~typed-buffer) (->cpp-datatype (.datatype ~typed-buffer)) (long ~src-offset)
            ~dest (long ~dest-offset) (long ~elem-count))))


(extend-type (Class/forName "[B")
  TypedBufferIO
  (copy-to-typed-buffer! [src src-offset typed-buffer dest-offset elem-count]
    (array-copy-to-impl ^bytes src src-offset ^TypedBuffer typed-buffer dest-offset elem-count))
  (copy-from-typed-buffer! [dest dest-offset typed-buffer src-offset elem-count]
    (array-copy-from-impl ^TypedBuffer typed-buffer src-offset ^bytes dest dest-offset elem-count)))

(extend-type (Class/forName "[S")
  TypedBufferIO
  (copy-to-typed-buffer! [src src-offset typed-buffer dest-offset elem-count]
    (array-copy-to-impl ^shorts src src-offset ^TypedBuffer typed-buffer dest-offset elem-count))
  (copy-from-typed-buffer! [dest dest-offset ^TypedBuffer typed-buffer src-offset elem-count]
    (array-copy-from-impl ^TypedBuffer typed-buffer src-offset ^shorts dest dest-offset elem-count)))

(extend-type (Class/forName "[I")
  TypedBufferIO
  (copy-to-typed-buffer! [src src-offset typed-buffer dest-offset elem-count]
    (array-copy-to-impl ^ints src src-offset ^TypedBuffer typed-buffer dest-offset elem-count))
  (copy-from-typed-buffer! [dest dest-offset typed-buffer src-offset elem-count]
    (array-copy-from-impl ^TypedBuffer typed-buffer src-offset ^ints dest dest-offset elem-count)))

(extend-type (Class/forName "[J")
  TypedBufferIO
  (copy-to-typed-buffer! [src src-offset typed-buffer dest-offset elem-count]
    (array-copy-to-impl ^longs src src-offset ^TypedBuffer typed-buffer dest-offset elem-count))
  (copy-from-typed-buffer! [dest dest-offset typed-buffer src-offset elem-count]
    (array-copy-from-impl ^TypedBuffer typed-buffer src-offset ^longs dest dest-offset elem-count)))

(extend-type (Class/forName "[F")
  TypedBufferIO
  (copy-to-typed-buffer! [src src-offset typed-buffer dest-offset elem-count]
    (array-copy-to-impl ^floats src src-offset ^TypedBuffer typed-buffer dest-offset elem-count))
  (copy-from-typed-buffer! [dest dest-offset typed-buffer src-offset elem-count]
    (array-copy-from-impl ^TypedBuffer typed-buffer src-offset ^floats dest dest-offset elem-count)))

(extend-type (Class/forName "[D")
  TypedBufferIO
  (copy-to-typed-buffer! [src src-offset typed-buffer dest-offset elem-count]
    (array-copy-to-impl ^doubles src src-offset ^TypedBuffer typed-buffer dest-offset elem-count))
  (copy-from-typed-buffer! [dest dest-offset typed-buffer src-offset elem-count]
    (array-copy-from-impl ^TypedBuffer typed-buffer src-offset ^doubles dest dest-offset elem-count)))


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


(defmacro array-view-copy-to-impl
  [src src-offset typed-buffer dest-offset elem-count]
  `(let [data# (.data ~src)
         real-offset# (.index ~src ~src-offset)]
     (check-buffer-access (.length ~src) ~src-offset ~elem-count)
     (array-copy-to-impl data# real-offset# ~typed-buffer ~dest-offset ~elem-count)))


(defmacro array-view-copy-from-impl
  [dest dest-offset typed-buffer src-offset elem-count]
  `(let [data# (.data ~dest)
         real-offset# (.index ~dest ~dest-offset)]
     (check-buffer-access (.length ~dest) ~dest-offset ~elem-count)
     (array-copy-from-impl ~typed-buffer ~src-offset data# real-offset# ~elem-count)))


(extend-protocol TypedBufferIO
  ByteArrayView
  (copy-to-typed-buffer! [src src-offset typed-buffer dest-offset elem-count]
    (array-view-copy-to-impl ^ByteArrayView src src-offset ^TypedBuffer typed-buffer dest-offset elem-count))
  (copy-from-typed-buffer! [dest dest-offset typed-buffer src-offset elem-count]
    (array-view-copy-from-impl ^ByteArrayView dest dest-offset ^TypedBuffer typed-buffer src-offset elem-count))
  ShortArrayView
  (copy-to-typed-buffer! [src src-offset typed-buffer dest-offset elem-count]
    (array-view-copy-to-impl ^ShortArrayView src src-offset ^TypedBuffer typed-buffer dest-offset elem-count))
  (copy-from-typed-buffer! [dest dest-offset typed-buffer src-offset elem-count]
    (array-view-copy-from-impl ^ShortArrayView dest dest-offset ^TypedBuffer typed-buffer src-offset elem-count))
  IntArrayView
  (copy-to-typed-buffer! [src src-offset typed-buffer dest-offset elem-count]
    (array-view-copy-to-impl ^IntArrayView src src-offset ^TypedBuffer typed-buffer dest-offset elem-count))
  (copy-from-typed-buffer! [dest dest-offset typed-buffer src-offset elem-count]
    (array-view-copy-from-impl ^IntArrayView dest dest-offset ^TypedBuffer typed-buffer src-offset elem-count))
  LongArrayView
  (copy-to-typed-buffer! [src src-offset typed-buffer dest-offset elem-count]
    (array-view-copy-to-impl ^LongArrayView src src-offset ^TypedBuffer typed-buffer dest-offset elem-count))
  (copy-from-typed-buffer! [dest dest-offset typed-buffer src-offset elem-count]
    (array-view-copy-from-impl ^LongArrayView dest dest-offset ^TypedBuffer typed-buffer src-offset elem-count))
  FloatArrayView
  (copy-to-typed-buffer! [src src-offset typed-buffer dest-offset elem-count]
    (array-view-copy-to-impl ^FloatArrayView src src-offset ^TypedBuffer typed-buffer dest-offset elem-count))
  (copy-from-typed-buffer! [dest dest-offset typed-buffer src-offset elem-count]
    (array-view-copy-from-impl ^FloatArrayView dest dest-offset ^TypedBuffer typed-buffer src-offset elem-count))
  DoubleArrayView
  (copy-to-typed-buffer! [src src-offset typed-buffer dest-offset elem-count]
    (array-view-copy-to-impl ^DoubleArrayView src src-offset ^TypedBuffer typed-buffer dest-offset elem-count))
  (copy-from-typed-buffer! [dest dest-offset typed-buffer src-offset elem-count]
    (array-view-copy-from-impl ^DoubleArrayView dest dest-offset ^TypedBuffer typed-buffer
                               src-offset elem-count))
  Object
  (copy-to-typed-buffer! [src src-offset dest dest-offset elem-count]
    (dtype/generic-copy! src src-offset dest dest-offset elem-count))
  (copy-from-typed-buffer! [dest dest-offset src src-offset elem-count]
    (dtype/generic-copy! src src-offset dest dest-offset elem-count)))


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
            (dtype/copy! src-data 0 buf-data 0 data-len)
            retval))]
    (resource/track retval)))


(defn typed-buffer-time-test
  [n-elems]
  (resource/with-resource-context
   (let [double-ary (double-array (range n-elems))
         buf (make-typed-buffer :float n-elems)]
     (time
      (dotimes [iter 100]
        (dtype/copy! double-ary 0 buf 0 n-elems))))))


(defn nio-buffer-time-test
  [n-elems]
  (resource/with-resource-context
    (let [double-ary (double-array (range n-elems))
          ^FloatBuffer buf (dtype/make-buffer :float n-elems)
          n-elems (long n-elems)]
      ;;I am hugely cheating here because I know the types.  It would take a lot of effort
      ;;to generically make this work across the different combinations of buffers and arrays.
      (time
       (dotimes [iter 100]
         (c-for [idx 0 (< idx n-elems) (inc idx)]
                (.put buf idx (float (aget double-ary idx)))))))))


(defn typed-buffer-same-type-time-test
  [n-elems]
  (resource/with-resource-context
   (let [double-ary (double-array (range n-elems))
         buf (make-typed-buffer :double n-elems)]
     (time
      (dotimes [iter 100]
        (dtype/copy! double-ary 0 buf 0 n-elems))))))


(defn nio-buffer-same-type-time-test
  [n-elems]
  (resource/with-resource-context
    (let [double-ary (double-array (range n-elems))
          buf (dtype/make-buffer :double n-elems)]
      (time
       (dotimes [iter 100]
         (dtype/copy! double-ary 0 buf 0 n-elems))))))


(defn run-time-tests
  []
  (let [n-elems 100000]
    (println "typed-buffer-marshal-test")
    (typed-buffer-time-test n-elems)
    (println "nio-buffer-marshal-test")
    (nio-buffer-time-test n-elems)
    (println "typed-buffer-same-type-time-test")
    (typed-buffer-same-type-time-test n-elems)
    (println "new-buffer-same-type-time-test")
    (nio-buffer-same-type-time-test n-elems)))
