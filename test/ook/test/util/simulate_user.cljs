(ns ook.test.util.simulate-user
  (:require
   [re-frame.core :as rf]))

(defn click-facet [facet]
  (rf/dispatch [:ui.facets/set-current facet]))

(defn cancel-current-selection []
  (rf/dispatch [:ui.facets/cancel-current-selection]))

(defn expand-codelist [codelist]
  (rf/dispatch [:ui.facets.current/toggle-codelist codelist]))
