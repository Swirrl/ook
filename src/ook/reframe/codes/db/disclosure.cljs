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

(defn- any-immediate-children-selected? [selected-codes node]
  (let [child-uris (set (map :ook/uri (:children node)))]
    (seq (set/intersection selected-codes child-uris))))

(defn- find-open-codes [db facet-name selection codelist-uri]
  (let [selected-codes (get selection codelist-uri)
        concept-tree (db/get-concept-tree db facet-name codelist-uri)
        find-parents-of-expanded-codes
        (fn walk* [path-to-node node]
          (let [parent-uri (:ook/uri node)
                children (:children node)]
            (when-not (keyword? children)
              (let [path (conj path-to-node parent-uri)]
                (if (any-immediate-children-selected? selected-codes node)
                  (concat path (mapcat (partial walk* path) children))
                  (mapcat (partial walk* path) children))))))]

    (set (mapcat (partial find-parents-of-expanded-codes #{}) concept-tree))))

(defn open-codelist-uris [selection]
  (->> selection (filter (fn [[_k v]] (seq v))) keys set))

(defn expand-selected-codes-for-codelist [current-disclosure db selection facet-name codelist-uri]
  (let [open-codes (find-open-codes db facet-name selection codelist-uri)
        expanded-uris (set (cons codelist-uri open-codes))]
    (apply (fnil conj #{}) current-disclosure expanded-uris)))

(defn expand-all-selected-codes [current-disclosure db selection facet-name]
  (let [open-codelists (open-codelist-uris selection)
        open-codes (mapcat (partial find-open-codes db facet-name selection) open-codelists)
        expanded-uris (set (concat open-codelists open-codes))]
    (apply (fnil conj #{}) current-disclosure expanded-uris)))
