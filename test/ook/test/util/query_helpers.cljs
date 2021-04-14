(ns ook.test.util.query-helpers
  (:require ["@testing-library/react" :as rt]))

(defn find-query
  ([selector]
   (find-query js/document selector))
  ([container selector]
   (.querySelector container selector)))

(defn find-text
  ([text]
   (.getByText rt/screen text))
  ([container text]
   (rt/getByText container text)))

(defn query-text
  ([text]
   (.queryByText rt/screen text))
  ([container text]
   (rt/queryByText container text)))

(defn find-all-query
  ([selector]
   (find-all-query js/document selector))
  ([container selector]
   (.querySelectorAll container selector)))

(defn find-all-text
  ([text]
   (.getAllByText rt/screen text))
  ([container text]
   (rt/getAllByText container text)))

(defn text-content [el]
  (.-textContent el))

(defn all-text-content
  ([selector]
   (all-text-content js/document selector))
  ([container selector]
   (->> (find-all-query container selector)
        (map text-content))))

(defn disabled? [el]
  (.-disabled el))

;;;;;;; OOK-specific UI helpers

(defn find-expand-toggle [label]
  (-> (find-text label)
      .-parentNode
      (find-query "button")))

(def code-label-query ".filters input[type='checkbox'] + label")

(defn all-labels []
  (all-text-content code-label-query))

(defn all-selected-labels []
  (all-text-content ".filters input[type='checkbox']:checked + label"))

(defn expanded-labels-under-label [label]
  (-> label find-text .-parentNode (all-text-content code-label-query)))

(defn select-any-button [label]
  (-> label find-text .-parentNode (find-text "any")))

(defn all-children-button [label]
  (-> label find-text .-parentNode (find-text "all children")))

(defn cancel-facet-selection-button []
  (find-query ".filters button.btn-close"))

;;; Dataset table

(defn apply-filter-button []
  (query-text "Apply filter"))

(defn all-dataset-titles []
  (-> (find-query ".ook-datasets") (all-text-content ".title-column strong")))

(defn dataset-count-text []
  (some-> (find-query ".filters") .-nextElementSibling text-content))

(defn datset-results-columns []
  (all-text-content ".ook-datasets th"))

(defn all-available-facets []
  (-> (find-text "Add a filter") .-nextElementSibling (all-text-content "button")))

(defn remove-facet-button [facet-name]
  (some-> (query-text facet-name) .-parentNode (find-query "button")))
