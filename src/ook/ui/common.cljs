(ns ook.ui.common)

(defn loading-wrapper [state & children]
  (if (= @state :loading)
    [:div "Loading..."]
    [:<>
     (for [[i child] (map-indexed vector children)]
       (with-meta child {:key i}))]))
