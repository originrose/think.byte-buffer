(defproject thinktopic/think.byte-buffer "0.1.0-SNAPSHOT"
  :description "Low level manipulation of byte data."
  :url "github.com/thinktopic/think.byte-buffer"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.bytedeco/javacpp "1.2.4"]
                 [thinktopic/resource "1.1.0"]
                 [thinktopic/think.datatype "0.3.3-SNAPSHOT"]]
  :java-source-paths ["java"]
  :native-path "java/native/"
  :aot [think.byte-buffer.main]
  :main think.byte-buffer.main)
