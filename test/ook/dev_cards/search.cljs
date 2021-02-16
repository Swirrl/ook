(ns ook.dev-cards.search
  (:require [reagent.core :as r]
            [ook.ui.search :as search]
            [devcards.core :as dc :include-macros true :refer [defcard-rg deftest]]
            [cljs.test :refer-macros [testing is]]
            [ook.dev-cards.state :as state]
            [ook.test.util.event-helpers :as eh]
            [ook.test.util.query-helpers :as qh]
            ;; ["@testing-library/user-event" :as ue]
            ))

(defcard-rg search
  search/ui
  (r/atom state/initial-state))

;; Elements

(defn search-input []
  (qh/find-query "input#search"))

(defn search-button []
  (qh/find-query "form#search button[type='submit']"))

;; (deftest searching
;;   (testing "pressing enter submits the search"
;;     (eh/set-input-val (search-input) "cars"))

;;   (testing "clicking the button submits the search"
;;     (eh/set-input-val (search-input) "cars")
;;     (eh/click (search-button))))
