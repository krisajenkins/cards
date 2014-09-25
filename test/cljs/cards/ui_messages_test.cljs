(ns cards.ui-messages-test
  (:require-macros [cemerick.cljs.test :refer [is deftest with-test run-tests testing test-var]])
  (:require [cemerick.cljs.test :as t]
            [cards.ui-messages :refer [add-person]]
            [datascript :as ds]))

(deftest add-person-test
  (let [db (ds/create-conn)]
    (add-person db {:name "Rip Van Winkle"
                    :age 100})
    (is (= #{["Rip Van Winkle" 100]}
           (ds/q '{:find [?name ?age]
                   :where [[?e :name ?name]
                           [?e :age ?age]]}
                 @db)))))

(t/test-ns 'cards.ui-messages-test)
