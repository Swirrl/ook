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
                                            (assoc facet ;; probably stop doing this and get the codes
                                                   ;; for a given facet when selected instead
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
  [{:label "Standard Industrial Trade Commodities v4" :disabled? true :allow-any? true
    :children [{:label "Animal & vegetable oils, fats & waxes" :disabled? true
                :children [{:label "Animal oils and fats and their fractions, nes, whether or not refined, but not chemically modified"}
                           {:label "Fats of bovine animals, sheep or goats, raw or rendered, whether or not pressed or solvent-extracted"}
                           {:label "Wool grease, crude" :disabled? true}]}
               {:label "Beverages & tobacco" :disabled? true
                :children [{:label "Beverages" :disabled? true
                            :children [{:label "Beer made from malt (including ale, stout and porter)"}
                                       {:label "Fermented beverages, nes (eg cider, perry, mead)"}]}]}
               {:label "Chemicals & related products, nes" :disabled true
                :children [] ;; to provide disclosed example
                }]}
   {:label "Product" :disabled? true :allow-any? true
    :children [{:label "Total" :disabled? true
                :children [{:label "A Products of agriculture, forestry & fishing" :disabled? true
                            :children [{:label "01 Products of agriculture"}
                                       {:label "02 Forestry products"}]}]}]}])

(defmethod ig/init-key :ook.handler/codes [_ {:keys [search/db]}]
  (fn [request]
    (if (requesting-transit? request)
      (transit-response cl-data) ;; get real data for the give codelists/top level codes here
      invalid-format-response)))

;; (defn- icon [& elements]
;;   [:svg.me-1 {:xmlns "http://www.w3.org/2000/svg" :width 16 :height 16 :fill "black" :viewBox "0 0 16 16"}
;;    elements])

;; (def icon-down
;;   (icon
;;    [:path {:d "M7.247 11.14 2.451 5.658C1.885 5.013 2.345 4 3.204 4h9.592a1 1 0 0 1 .753 1.659l-4.796 5.48a1 1 0 0 1-1.506 0z"}]))

;; (def icon-up
;;   (icon
;;    [:path {:d "m12.14 8.753-5.482 4.796c-.646.566-1.658.106-1.658-.753V3.204a1 1 0 0 1 1.659-.753l5.48 4.796a1 1 0 0 1 0 1.506z"}]))

;; (declare list-group-item)

;; (defn- list-group [elements]
;;   [:ul.list-group-flush
;;    (map list-group-item elements)])

;; (defn- list-group-item [{:keys [label children disabled? allow-any?] :as element}]
;;   (let [id (keyword label)]
;;     [:li.list-group-item
;;      (when children
;;        (if (empty? children) ;; this would actually be controlled by disclosure interaction
;;          icon-up
;;          icon-down))
;;      [:input.form-check-input.me-1 (merge {:type "checkbox" :id id} (when disabled? {:disabled true}))]
;;      [:label.form-check-label {:for id} label]
;;      (when children
;;        [:a.ms-1.link-primary (if allow-any? "any" "all children")])
;;      (when children
;;        (list-group children))]))

;; (defn- codelist-example []
;;   [:div
;;    (list-group cl-data)])
