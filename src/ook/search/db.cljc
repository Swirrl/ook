(ns ook.search.db)

(defprotocol SearchBackend
  (get-codes [db uris]
    "Retrieve codes by URI")

  (search-codes [db params]
    "Search for codes with a label matching the given term")

  (get-components [db uris]
    "Retrieve components by URI")

  (components->codelists [db component-uris]
    ("Retrieve codelist URIs from component URIs"))

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

  (get-concept-tree [db codelist]
    "Resolve concept tree for codelist"))
