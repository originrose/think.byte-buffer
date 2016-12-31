(ns think.byte-buffer.compile
  (:gen-class)
  (:import [org.bytedeco.javacpp.tools Builder]))


(defn build-java-stub
  []
  (Builder/main (into-array String ["think.byte_buffer.presets.ByteBuffer" "-d" "java"])))


(defn build-jni-lib
  []
  (Builder/main (into-array String ["think.byte_buffer.ByteBuffer" "-d"
                                    (str (System/getProperty "user.dir")
                                         "/java/native/linux/x86_64")
                                    "-nodelete" ;;When shit doesn't work this is very helpful
                                    "-Xcompiler"
                                    (str "-I" (System/getProperty "user.dir")
                                         "/cpp")
                                    "-Xcompiler"
                                    "-std=c++14"
                                    ])))

(defn -main
  [& args]
  (let [arg-val (first args)
        command (if arg-val
                  (keyword arg-val)
                  :build-jni-java)]
    (condp = command
      :build-jni-java ;;step 1
      (build-java-stub)
      :build-jni
      (build-jni-lib))))
