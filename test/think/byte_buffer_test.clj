(ns think.byte-buffer-test
  (:require [think.byte-buffer :as bb]
            [think.datatype.core :as dtype]
            [clojure.test :refer :all]
            [think.resource.core :as resource]
            [think.datatype.time-test :as time-test])
  (:import [org.bytedeco.javacpp DoublePointer FloatPointer]))


(defn time-op
  [op]
  (dotimes [iter 5]
    (op))
  (time
   (dotimes [iter 100]
     (op))))


(defn typed-buffer-time-test
  [n-elems]
  (resource/with-resource-context
   (let [double-ary (double-array (range n-elems))
         buf (bb/make-typed-buffer :float n-elems)]
     (time-op #(dtype/copy! double-ary 0 buf 0 n-elems)))))


(defn typed-buffer-reverse-time-test
  [n-elems]
  (resource/with-resource-context
   (let [src (bb/make-typed-buffer :double (range n-elems))
         dest (float-array n-elems)]
     (time-op #(dtype/copy! src 0 dest 0 n-elems)))))


(defn nio-buffer-time-test
  [n-elems]
  (resource/with-resource-context
    (let [double-ary (double-array (range n-elems))
          ^FloatBuffer buf (dtype/make-buffer :float n-elems)
          n-elems (long n-elems)]
      (time-op #(dtype/copy! double-ary 0 buf 0 n-elems)))))


(defn javacpp-pointer-time-test
  [n-elems]
  (resource/with-resource-context
    (let [double-ary (double-array (range n-elems))
          ^FloatPointer ptr (FloatPointer. (long n-elems))
          buf (.asBuffer ptr)
          n-elems (long n-elems)]
      (time-op
       #(dtype/copy! double-ary 0 buf 0 n-elems)))))


(defn typed-buffer-same-type-time-test
  [n-elems]
  (resource/with-resource-context
   (let [double-ary (double-array (range n-elems))
         buf (bb/make-typed-buffer :double n-elems)]
     (time-op
      #(dtype/copy! double-ary 0 buf 0 n-elems)))))


(defn nio-buffer-same-type-time-test
  [n-elems]
  (resource/with-resource-context
    (let [double-ary (double-array (range n-elems))
          buf (dtype/make-buffer :double n-elems)]
      (time-op #(dtype/copy! double-ary 0 buf 0 n-elems)))))


(deftest run-time-tests
  (let [n-elems 100000]
    (println "typed-buffer-marshal-test")
    (typed-buffer-time-test n-elems)
    (println "typed-buffer-reverse-marshal-test")
    (typed-buffer-reverse-time-test n-elems)
    (println "nio-buffer-marshal-test")
    (nio-buffer-time-test n-elems)
    (println "javacpp marshal test")
    (javacpp-pointer-time-test n-elems)
    (println "typed-buffer-same-type-time-test")
    (typed-buffer-same-type-time-test n-elems)
    (println "new-buffer-same-type-time-test")
    (nio-buffer-same-type-time-test n-elems)
    (println "float array -> double array fast path")
    (time-test/datatype-copy-time-test)
    (println "float array -> double array view fast path")
    (time-test/array-into-view-time-test)))
