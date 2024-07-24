(defproject retailcal "0.1.0-SNAPSHOT"
  :description "A library to convert Gregorian to retail calendars, e.g. the National Retail Federation's 454 calendar"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [clojure.java-time "1.4.2"]
                 [scicloj/tablecloth "7.029.2"]]
  :main ^:skip-aot retailcal.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
