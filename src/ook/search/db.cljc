(ns ook.search.db)

(defprotocol SearchBackend
  (get-codes [c query]
    "Return a search result with all codes that have the given term in their label."))
