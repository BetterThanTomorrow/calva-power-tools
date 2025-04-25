;; Dummy test to keep the unit test infra alive

(ns calva-power-tools.example-test
  (:require
   [clojure.test :refer [deftest is testing]]))

(deftest example
  (testing "Exemple"
    (is (= 42
           42)
        "The meaning of life")))