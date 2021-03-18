(ns ook.util)

(defn pluralize
  "Naive pluralization -- only adds 's', doesn't work for many English words!"
  [word count]
  (cond-> word
    (or (= 0 count) (> count 1)) (str "s")))

(defn box [v]
  (if (coll? v) v [v]))

(defn mjoin
  "Merge 2 sequences of hash-maps, joined by a shared key"
  [s1 s2 k]
  (map #(apply merge %)
       (vals (group-by k (concat s1 s2)))))
