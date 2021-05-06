(ns ook.reframe.codes.db.disclosure
  (:require
   [ook.reframe.db :as db]
   [clojure.set :as set]))

(defn expanded? [facet uri]
  (-> facet :expanded (get uri) boolean))

(defn expand [facet uri]
  (update facet :expanded (fnil conj #{}) uri))

(defn collapse [facet uri]
  (update facet :expanded disj uri))

(defn toggle [facet uri]
  (if (expanded? facet uri)
    (collapse facet uri)
    (update facet :expanded (fnil conj #{}) uri)))

(defn- any-immediate-children-included? [target-codes node]
  (let [child-uris (set (map :ook/uri (:children node)))]
    (seq (set/intersection target-codes child-uris))))

(defn find-open-codes [target-codes tree]
  (let [find-parents-of-expanded-codes
        (fn walk* [path-to-node node]
          (let [parent-uri (:ook/uri node)
                children (:children node)]
            (when-not (keyword? children)
              (let [path (conj path-to-node parent-uri)]
                (if (any-immediate-children-included? target-codes node)
                  (concat path (mapcat (partial walk* path) children))
                  (mapcat (partial walk* path) children))))))]
    (set (mapcat (partial find-parents-of-expanded-codes #{}) tree))))

(defn open-codelist-uris [selection]
  (->> selection (filter (fn [[_k v]] (seq v))) keys set))

(defn expand-selected-codes-for-codelist [current-disclosure db selection facet-name codelist-uri]
  (let [concept-tree (db/get-concept-tree db facet-name codelist-uri)
        selected-codes (get selection codelist-uri)
        open-codes (find-open-codes selected-codes concept-tree)
        expanded-uris (set (cons codelist-uri open-codes))]
    (apply (fnil conj #{}) current-disclosure expanded-uris)))

(defn expand-all-selected-codes [current-disclosure db selection facet-name]
  (let [open-codelists (open-codelist-uris selection)
        open-codes (mapcat (fn [codelist-uri]
                             (let [selected-codes (get selection codelist-uri)
                                   concept-tree (db/get-concept-tree db facet-name codelist-uri)]
                               (find-open-codes selected-codes concept-tree)))
                           open-codelists)
        expanded-uris (set (concat open-codelists open-codes))]
    (apply (fnil conj #{}) current-disclosure expanded-uris)))
