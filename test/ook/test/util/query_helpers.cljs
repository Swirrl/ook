(ns ook.test.util.query-helpers
  (:require ["@testing-library/react" :as rt]
            [ook.ui.icons :as icons]))

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

(defn find-expansion-toggle [label]
  (-> (find-text label)
      .-parentNode
      (find-query "button")))

(defn find-toggle-icon-path [expand-toggle]
  (-> expand-toggle (find-query "svg path") (.getAttribute "d")))

(defn open? [expand-toggle]
  (= icons/down-path (find-toggle-icon-path expand-toggle)))

(defn closed? [expand-toggle]
  (= icons/up-path (find-toggle-icon-path expand-toggle)))

(def code-label-query ".filters input[type='checkbox'] + label")

(defn all-labels []
  (all-text-content code-label-query))

(defn all-selected-labels []
  (all-text-content ".filters input[type='checkbox']:checked + label"))

(defn expanded-labels-under-label [label]
  (-> label query-text .-parentNode (all-text-content code-label-query)))

(defn select-any-button [label]
  (-> label query-text .-parentNode (find-text "any")))

(defn multi-select-button [label]
  (some-> label query-text .-parentNode (find-query "button ~ button")))

(defn cancel-facet-selection-button []
  (find-query ".filters button.btn-close"))

;;; Dataset table

(defn apply-filter-button []
  (query-text "Apply filter"))

(defn all-dataset-titles []
  (-> (find-query ".ook-datasets") (all-text-content ".title-column strong")))

(defn column-x-contents [column-index-starting-from-1]
  (all-text-content (str ".ook-datasets tr td:nth-child(" column-index-starting-from-1 ")")))

(defn dataset-count-text []
  (some-> (find-query ".filters") .-nextElementSibling text-content))

(defn datset-results-columns []
  (all-text-content ".ook-datasets th"))

(defn all-available-facets []
  (-> (find-text "Add a filter") .-nextElementSibling (all-text-content "button")))

(defn remove-facet-button [facet-name]
  (some-> (query-text facet-name) .-parentNode (find-query "button")))
