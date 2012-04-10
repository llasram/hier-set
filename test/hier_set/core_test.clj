(ns hier-set.core-test
  (:require [hier-set.core :as hs])
  (:use [hier-set.core :only [hier-set hier-set-by]])
  (:use [clojure.test]))

(defn with-starts?
  "Does string s begin with the provided prefix?"
  {:inline (fn [prefix s & to]
             `(let [^String s# ~s, ^String prefix# ~prefix]
                (.startsWith s# prefix# ~@(when (seq to) [`(int ~@to)]))))
   :inline-arities #{2 3}}
  ([prefix s] (.startsWith ^String s ^String prefix))
  ([prefix s to] (.startsWith ^String s ^String prefix (int to))))

(deftest test-hier-set
  (let [hs (hier-set with-starts? "foo" "foo.bar" "foo.bar.baz" "quux")]
    (testing "Able to retrieve ancestor primary elements"
      (testing "using `get`"
        (is (= nil (get hs "bar")))
        (is (= '("foo") (get hs "foo.baz")))
        (is (= '("foo.bar" "foo") (get hs "foo.bar.bar"))))
      (testing "by invoking the set as a function"
        (is (= nil (hs "bar")))
        (is (= '("foo") (hs "foo.baz")))
        (is (= '("foo.bar" "foo") (hs "foo.bar.bar"))))
      (testing "using `ancestors`"
        (is (= '() (hs/ancestors hs "bar")))
        (is (= '("foo") (hs/ancestors hs "foo.baz")))
        (is (= '("foo.bar" "foo") (hs/ancestors hs "foo.bar.bar")))))
    (testing "Able to retrieve descendant primary elements"
      (testing "using `descendants`"
        (is (= '() (hs/descendants hs "bar")))
        (is (= '("foo.bar" "foo.bar.baz") (hs/descendants hs "foo.bar")))))))
