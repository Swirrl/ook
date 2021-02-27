(ns ook.reframe.views
  (:require
   [ook.reframe.views.search :as search]))

(defn main []
  (search/create-filter-card))
