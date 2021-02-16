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
