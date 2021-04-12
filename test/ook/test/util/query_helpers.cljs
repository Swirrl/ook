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
