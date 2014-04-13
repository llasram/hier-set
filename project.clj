(defproject hier-set "1.2.0-SNAPSHOT"
  :description "A Clojure hierarchical set."
  :url "http://github.com/llasram/hier-set"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :global-vars {*warn-on-reflection* true}
  :dependencies [[org.clojure/clojure "1.6.0"]]
  :aliases {"all" ["with-profile" ~(str "+clojure-1-4:"
                                        "+clojure-1-5:"
                                        "+clojure-1-6")]}
  :profiles {:clojure-1-4 {:dependencies
                           [[org.clojure/clojure "1.4.0"]]}
             :clojure-1-5 {:dependencies
                           [[org.clojure/clojure "1.5.1"]]}
             :clojure-1-6 {:dependencies
                           [[org.clojure/clojure "1.6.0"]]}})
