(ns ook.test.util.query-helpers
  (:require ["@testing-library/react" :as rt]
            [ook.ui.icons :as icons]
            [clojure.string :as str]))

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
  (let [toggle (some-> (query-text label) .-parentNode (find-query "button"))]
    (if toggle
      toggle
      (js/console.error "Could not find toggle for option: " label))))

(defn find-toggle-icon-path [expand-toggle]
  (-> expand-toggle (find-query "svg path") (.getAttribute "d")))

(defn open? [expand-toggle]
  (= icons/down-path (find-toggle-icon-path expand-toggle)))

(defn closed? [expand-toggle]
  (= icons/up-path (find-toggle-icon-path expand-toggle)))

(def selectable-code-label-query ".filters input[type='checkbox'] + label")

(defn all-checkbox-labels []
  (all-text-content selectable-code-label-query))

(defn all-labels []
  (all-text-content (str ".filters .child > span, " selectable-code-label-query)))

(defn all-selected-labels []
  (all-text-content ".filters input[type='checkbox']:checked + label"))

(defn expanded-labels-under-label [label]
  (-> label query-text .-parentNode (all-text-content selectable-code-label-query)))

(defn select-any-button [label]
  (-> label query-text .-parentNode (find-text "any")))

(defn multi-select-button [label]
  (some-> label query-text .-parentNode (find-query "button ~ button")))

(defn cancel-facet-selection-button []
  (find-query ".filters button.btn-close"))

(defn editable-facet-button [facet-name]
  (-> (find-query ".filters") (query-text facet-name) (find-query "svg")))

;;; Search

(defn code-search-input []
  (find-query "#search-term"))

(defn submit-search-button []
  (find-query "#search button[type='submit']"))

(defn search-input-val []
  (.-value (code-search-input)))

;;; Dataset table

(defn apply-filter-button []
  (query-text "Apply filter"))

(defn remove-filter-button []
  (query-text "Remove filter"))

(defn all-dataset-titles []
  (-> (find-query ".ook-datasets") (all-text-content ".title-column strong")))

(defn column-x-contents [column-index-starting-from-1]
  (all-text-content (str ".ook-datasets tr td:nth-child(" column-index-starting-from-1 ")")))

(defn dataset-count-text []
  (some-> (find-query ".ook-datasets h2") text-content))

(defn datset-results-columns []
  (all-text-content ".ook-datasets th"))

(defn all-available-facets []
  (-> (find-query ".filters h2") .-nextElementSibling .-firstElementChild (all-text-content "button")))

(defn remove-facet-button [facet-name]
  (some-> (find-query ".ook-datasets") (query-text facet-name) .-parentNode (find-query "button")))

(defn applied-facets []
  (->> (find-all-query ".ook-datasets th") (drop 1) (map text-content) (remove str/blank?)))
