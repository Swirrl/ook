(ns ook.handler
  (:require [integrant.core :as ig]
            [ring.util.response :as resp]
            [ook.ui.layout :as layout]
            [ook.search.db :as db]
            [ook.params.parse :as p]
            [ook.concerns.transit :as t]))

;; App entry handler

(defmethod ig/init-key :ook.handler/main [_ {:keys [search/db]}]
  (fn [_request]
    (let [facets-with-codelists (->> (db/get-facets db)
                                     (map (fn [facet]
                                            (assoc facet
                                                   :codelists
                                                   (db/components->codelists
                                                    db
                                                    (:dimensions facet))))))]
      (resp/response (layout/->html (layout/main {:facets facets-with-codelists
                                                  :dataset-count (db/dataset-count db)}))))))

;;; Internal transit API

(defn- requesting-transit? [{:keys [headers]}]
  (let [accept (headers "accept")]
    (= "application/transit+json" accept)))

(def invalid-format-response
  {:status 406 :headers {} :body "Unsupported content type"})

(defn- transit-content-type [response]
  (-> response (resp/header "Content-Type" "application/transit+json")))

;; (defmethod ig/init-key :ook.handler/get-codes [_ {:keys [search/db]}]
;;   (fn [request]
;;     (if (requesting-transit? request)
;;       (let [query (or (p/get-query request) "")
;;             codes (db/get-codes db query)]
;;         (-> (resp/response (t/write-string codes))
;;             transit-content-type))
;;       invalid-format-response)))

(defn- transit-response [body]
  (-> body t/write-string resp/response transit-content-type))

(defmethod ig/init-key :ook.handler/datasets [_ {:keys [search/db]}]
  (fn [request]
    (if (requesting-transit? request)
      ;; Old implementation that applied a custom code selection.
      ;; When this comes back, combine it with other filter facets
      ;; (let [filters (p/parse-filters request)
      ;;       result (db/get-datasets db filters)]
      ;;   (-> (resp/response (t/write-string result))
      ;;       transit-content-type))
      (let [facets (p/get-facets request)
            datasets (if facets
                       (db/get-datasets-for-facets db facets)
                       (db/all-datasets db))]
        (transit-response datasets))
      invalid-format-response)))

(def cl-data
  [{:ook/uri "/def/code1" :label "Standard Industrial Trade Commodities v4" :disabled? true :allow-any? true
    :children [{:label "Animal & vegetable oils, fats & waxes" :disabled? true :ook/uri "/def/code2"
                :children [{:ook/uri "/def/code3"
                            :label "Animal oils and fats and their fractions, nes, whether or not refined, but not chemically modified"}
                           {:ook/uri "/def/code4"
                            :label "Fats of bovine animals, sheep or goats, raw or rendered, whether or not pressed or solvent-extracted"}
                           {:ook/uri "/def/code5"
                            :label "Wool grease, crude" :disabled? true}]}
               {:ook/uri "/def/code6" :label "Beverages & tobacco" :disabled? true
                :children [{:ook/uri "/def/code7" :label "Beverages" :disabled? true
                            :children [{:ook/uri "/def/code8" :label "Beer made from malt (including ale, stout and porter)"}
                                       {:ook/uri "/def/code9" :label "Fermented beverages, nes (eg cider, perry, mead)"}]}]}
              {:ook/uri "/def/code10" :label "Chemicals & related products, nes" :disabled true}]}
   {:ook/uri "/def/code11" :label "Product" :disabled? true :allow-any? true
    :children [{:ook/uri "/def/code12" :label "Total" :disabled? true
                :children [{:ook/uri "/def/code13" :label "A Products of agriculture, forestry & fishing" :disabled? true
                            :children [{:ook/uri "/def/code14" :label "01 Products of agriculture"}
                                       {:ook/uri "/def/code15" :label "02 Forestry products"}]}]}]}])

(defmethod ig/init-key :ook.handler/codes [_ {:keys [search/db]}]
  (fn [request]
    (if (requesting-transit? request)
      (let [top-uris (p/get-codelists request)
            trees (db/get-code-trees db top-uris)]
        (transit-response trees)
        ;; (transit-response cl-data)
) ;; get real data for the give codelists/top level codes here
      invalid-format-response)))
