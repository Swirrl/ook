(ns ook.util)

(defn pluralize
  "Naive pluralization -- only adds 's', doesn't work for many English words!"
  [word count]
  (cond-> word
    (or (= 0 count) (> count 1)) (str "s")))
