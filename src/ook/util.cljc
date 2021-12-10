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

(defn lookup
  "Turns a sequence of docs (hashmaps) into a lookup from key to doc.
  Duplicates clobber, latest wins."
  [key xs]
  (into {} (map (fn [x] [(get x key) x]) xs)))

(def id-lookup
  "Turns a sequence of docs (hashmaps) with :ook/uri attributes into a map from :ook/uri to the doc.
  Duplicates clobber, latest wins."
  (partial lookup :ook/uri))

(defn join-by
  "Joins two sequences of docs (hashmaps) x and y by x-key and y-key.
  Docs having the same key within a sequence clobber, latest wins.
  Docs having the same key between sequences are merged. If these share other keys the values from y win."
  [x y x-key y-key]
  (let [lookup-x (lookup x-key x)
        lookup-y (lookup y-key y)
        joined (merge-with merge lookup-x lookup-y)]
    (vals joined)))

(defn map-values [m f]
  (zipmap (keys m) (map f (vals m))))
