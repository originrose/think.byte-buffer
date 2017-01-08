(ns think.byte-buffer.compile
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
