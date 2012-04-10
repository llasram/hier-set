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

(deftest test-basic
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

(def ^:private testing-data
  ["adam" "adam.nested" "adam.nested.deeply"
   "betty"
   "david" "david.nested.deeply"
   "erin.nested"])

(deftest test-modification
  (let [orig (apply hier-set with-starts? testing-data)]
    (testing "Able to add elements to the set"
      (testing "with no existing relationship"
        (let [updated (conj orig "chris")]
          (is (= nil (get orig "chris")))
          (is (= '("chris") (get updated "chris")))))
      (testing "with existing ancestors"
        (let [updated (conj orig "betty.nested")
              test-key "betty.nested.deeply"]
          (is (= '("betty") (get orig test-key)))
          (is (= '("betty.nested" "betty") (get updated test-key)))))
      (testing "with existing descendants"
        (let [updated (conj orig "erin")
              test-key "erin.nested.deeply"]
          (is (= '("erin.nested") (get orig test-key)))
          (is (= '("erin.nested" "erin") (get updated test-key)))))
      (testing "with existing ancestors and descendants"
        (let [updated (conj orig "david.nested")
              test-key "david.nested.deeply"]
          (is (= '("david.nested.deeply" "david") (get orig test-key)))
          (is (= '("david.nested.deeply" "david.nested" "david")
                 (get updated test-key))))))
    (testing "Able to remove elements from the set"
      (testing "with no existing relationship"
        (let [updated (disj orig "betty")]
          (is (= '("betty") (get orig "betty")))
          (is (= nil (get updated "betty")))))
      (testing "with existing ancestors"
        (let [updated (disj orig "david.nested.deeply")
              test-key "david.nested.deeply"]
          (is (= '("david.nested.deeply" "david") (get orig test-key)))
          (is (= '("david") (get updated test-key)))))
      (testing "with existing descendants"
        (let [updated (disj orig "david")
              test-key "david.nested.deeply"]
          (is (= '("david.nested.deeply" "david") (get orig test-key)))
          (is (= '("david.nested.deeply") (get updated test-key)))))
      (testing "with existing ancestors and descendants "
        (let [updated (disj orig "adam.nested")
              test-key "adam.nested.deeply"]
          (is (= '("adam.nested.deeply" "adam.nested" "adam")
                 (get orig test-key)))
          (is (= '("adam.nested.deeply" "adam") (get updated test-key))))))))
