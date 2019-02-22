(ns puppetlabs.schema-tools-test
  (:require [clojure.test :refer :all]
            [schema.core :as sc]
            [puppetlabs.schema-tools :refer :all]))

(deftest explanation
  (let [value {:a 1 :b "hello"}
        schema {:a sc/Str :b sc/Str}
        error (sc/check schema value)
        data {:value value, :schema schema, :error error}]

    (testing "reports strings as `String` in schema and error explanations"
      (is (= {:schema {:a 'Str, :b 'Str}
              :value value
              :error {:a '(not (instance? String 1))}}
             (explain-and-simplify-exception-data data))))))
