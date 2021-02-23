(ns ook.dev-cards.search
  (:require [reagent.core :as r]
            [ook.ui.search :as search]
            [devcards.core :as dc :include-macros true :refer [defcard-rg deftest]]
            [cljs.test :refer-macros [testing is use-fixtures]]
            [ook.dev-cards.state :as state]
            [ook.test.util.event-helpers :as eh]
            [ook.test.util.query-helpers :as qh]))

;; (use-fixtures :once
;;   {:before (fn [] (dc/start-devcard-ui!))})

(defonce search-results (r/atom (:initial state/search-results)))

(defonce latest-search (atom nil))

(defn- fake-handler [event]
  (.preventDefault event)
  (let [query (-> event .-target js/FormData. (.get "q"))]
    (reset! latest-search query)
    (reset! search-results (state/search-results query))))

(defcard-rg search
  [search/ui search-results {:handler/submit-search fake-handler}]
  search-results)

;; Elements

(defn search-input []
  (qh/find-query "input#query"))

(defn search-button []
  (qh/find-query "form#search button[type='submit']"))

(defn result-text []
  (qh/text-content (qh/find-query "form#search + p")))

;; Tests

(deftest searching
  (reset! latest-search nil)

  (testing "pressing enter submits the search"
    (eh/set-input-val (search-input) "driver")
    (eh/press-enter (search-input))
    (is (= "driver" @latest-search)))

  (testing "clicking the button submits the search"
    (eh/set-input-val (search-input) "england")
    (eh/click (search-button))
    (is (= "england" @latest-search)))

  (testing "it shows the apply button when there are results"
    (is (not (nil? (qh/query-text "Apply filter")))))

  (testing "it shows something sensible when there are no results"
    (eh/set-input-val (search-input) "missing")
    (eh/click (search-button))
    (is (= "Found 0 codes matching \"missing\"" (result-text))))

  (testing "it does not show the apply button when there are no results"
    (is (nil? (qh/query-text "Apply filter")))))
