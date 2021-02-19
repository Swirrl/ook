(ns ook.search.db)

(defprotocol SearchBackend
  (get-codes [c query]
    "Return a search result with all codes that have the given term in their label.")

  (get-datasets [c codes]
    "Return a search result with all datasets that have the given codes"))
