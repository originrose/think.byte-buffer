(ns think.byte-buffer.main
  (:require [think.datatype.core :as dtype]
            [think.byte-buffer :as byte-buffer]
            [think.datatype.time-test :as time-test]
            [think.resource.core :as resource]
            [think.byte-buffer :as bb]
            [think.byte-buffer.compile :as compile])
  (:import [org.bytedeco.javacpp DoublePointer FloatPointer])
  (:gen-class))


(defn time-op
  [op]
  (time-test/time-test op))

(def n-elems 100000)

(defn typed-buffer-time-test
  []
  (resource/with-resource-context
   (let [double-ary (double-array (range n-elems))
         buf (bb/make-typed-buffer :float n-elems)]
     (time-op #(dtype/copy! double-ary 0 buf 0 n-elems)))))


(defn typed-buffer-reverse-time-test
  []
  (resource/with-resource-context
   (let [src (bb/make-typed-buffer :double (range n-elems))
         dest (float-array n-elems)]
     (time-op #(dtype/copy! src 0 dest 0 n-elems)))))


(defn nio-buffer-time-test
  []
  (resource/with-resource-context
    (let [double-ary (double-array (range n-elems))
          ^FloatBuffer buf (dtype/make-buffer :float n-elems)
          n-elems (long n-elems)]
      (time-op #(dtype/copy! double-ary 0 buf 0 n-elems)))))


(defn javacpp-pointer-time-test
  []
  (resource/with-resource-context
    (let [double-ary (double-array (range n-elems))
          ^FloatPointer ptr (FloatPointer. (long n-elems))
          buf (.asBuffer ptr)
          n-elems (long n-elems)]
      (time-op
       #(dtype/copy! double-ary 0 buf 0 n-elems)))))


(defn typed-buffer-same-type-time-test
  []
  (resource/with-resource-context
   (let [double-ary (double-array (range n-elems))
         buf (bb/make-typed-buffer :double n-elems)]
     (time-op
      #(dtype/copy! double-ary 0 buf 0 n-elems)))))


(defn nio-buffer-same-type-time-test
  []
  (resource/with-resource-context
    (let [double-ary (double-array (range n-elems))
          buf (dtype/make-buffer :double n-elems)]
      (time-op #(dtype/copy! double-ary 0 buf 0 n-elems)))))


(defn typed-buffer->array-indexed-generic
  []
  (let [src (bb/make-typed-buffer :float (range n-elems))
        src-indexes (int-array (range n-elems))
        dest (dtype/make-array-of-type :double n-elems)
        dst-indexes (int-array (reverse (range n-elems)))]
    (time-op #(dtype/generic-indexed-copy! src 0 src-indexes dest 0 dst-indexes))))



(defn perf-test
  []
  (println "typed-buffer-marshal-test")
  (typed-buffer-time-test)
  (println "typed-buffer-reverse-marshal-test")
  (typed-buffer-reverse-time-test)
  (println "nio-buffer-marshal-test")
  (nio-buffer-time-test)
  (println "javacpp marshal test")
  (javacpp-pointer-time-test)
  (println "typed-buffer-same-type-time-test")
  (typed-buffer-same-type-time-test)
  (println "new-buffer-same-type-time-test")
  (nio-buffer-same-type-time-test)
  (println "float array -> double array fast path")
  (time-test/datatype-copy-time-test)
  (println "float array -> double array view fast path")
  (time-test/array-into-view-time-test)
  (println "array->array-indexed-copy")
  (time-test/indexed-copy-time-test #(dtype/make-array-of-type :float %)
                                    #(dtype/make-array-of-type :double %))
  (println "typed-buffer->array-indexed-copy")
  (time-test/indexed-copy-time-test #(bb/make-typed-buffer :float %)
                                    #(dtype/make-array-of-type :double %))
  (println "typed-buffer->typed-buffer-indexed-copy")
  (time-test/indexed-copy-time-test #(bb/make-typed-buffer :float %)
                                    #(bb/make-typed-buffer :double %))
  (println "typed-buffer->array-indexed-generic")
  (typed-buffer->array-indexed-generic)
  (println "typed-buffer-marshal-test")
  (typed-buffer-time-test))


(defn -main
  [& args]
  (if-let [arg-val (first args)]
    (condp = (keyword arg-val)
      :build-jni-java ;;step 1
      (compile/build-java-stub)
      :build-jni
      (compile/build-jni-lib))
    (perf-test)))
