(ns ook.reframe.codes.db.disclosure)

(defn expanded? [facet uri]
  (-> facet :expanded (get uri) boolean))

(defn collapse [facet uri]
  (update facet :expanded disj uri))

(defn toggle [facet uri]
  (if (expanded? facet uri)
    (collapse facet uri)
    (update facet :expanded (fnil conj #{}) uri)))
