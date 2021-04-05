(ns ook.search.db)

(defprotocol SearchBackend
  ;; (get-codes [db query]
  ;;   "Return a search result with all codes that have the given term in their label.")

  (get-components [db uris]
    "Retrieve components by URI")

  (components->codelists [db component-uris]
    ("Retrieve codelist URIs from component URIs"))

  (get-datasets [db codes]
    "Return a search result with all datasets that have the given codes")

  (get-datasets-for-components [db components]
    "Retrieve datasets that use the components")

  (get-datasets-for-facets [db facets]
    "Retrieve datasets explaining how they match the facets")

  (all-datasets [db]
    "Return all datasets")

  (dataset-count [db]
    "Return a single number, the total number of datasets")

  (get-facets [db]
    "Resolve facets from configuration and database")

  (get-code-trees [db codelist-uris]
    "Resolve code trees for codelists, with the root codelist at the top of each tree"))
