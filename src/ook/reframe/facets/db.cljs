(ns ook.reframe.facets.db
  (:require
   [ook.reframe.db :as db]
   [clojure.set :as set]))

(defn set-current-facet [db facet]
  (let [status (if (empty? (:codelists facet)) :success/empty :success/ready)]
    (-> db
        (assoc :ui.facets/current facet)
        (assoc :ui.facets.current/status status))))

(defn- get-expanded-uris [db facet-name selection]
  ;; for each selected codelist
  ;; add its uri
  ;; if it has children, walk to tree, adding any uris whose children are included in the selection
  ;; (js/console.log )


  (->> selection
       (map (fn [[k v]] (when (seq v) k)))
       (remove nil?)
       (mapcat (fn [codelist-uri]
                 (let [concept-tree (db/get-concept-tree db facet-name codelist-uri)]
                   (cons codelist-uri (db/all-expandable-uris concept-tree)))))
       set)


  ;; (let [codelist-uris (keys selection)
  ;;       selected-uris (->> selection vals flatten (remove nil?) set)
  ;;       trees (-> db :facets/config (get facet-name) :codelists vals)
  ;;       find-expanded-parents (fn walk* [node]
  ;;                               (let [children (:children node)
  ;;                                     parent-uri (:ook/uri node)]
  ;;                                 (when-not (keyword? children)
  ;;                                   (if (seq (set/intersection children selected-uris))
  ;;                                     parent-uri
  ;;                                     (mapcat walk* children)))))

  ;;       expanded-parent-uris (mapcat find-expanded-parents trees)]
  ;;   (set (concat codelist-uris expanded-parent-uris)))
  )

(defn set-applied-selection-and-disclosure [db {:keys [name] :as facet}]
  (if-let [applied-selection (get-in db [:facets/applied name])]
    (-> facet
        (assoc :selection applied-selection)
        (assoc :expanded (get-expanded-uris db name applied-selection)))
    facet))
