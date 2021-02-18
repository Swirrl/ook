(ns ook.ui.common)

(defn siblings
  "Isomorphic way of putting siblings at the same level in the DOM.

  It takes a number of hiccup forms and in cljs wraps them in a
  reagent [:<> ,,,] vector, whilst in clj on the server, it returns
  them in a (list ,,,).

  This can be used to workaround various differences between clj and
  reagent flavoured hiccup."
  [& forms]
  #?(:clj (remove nil? (apply list forms))
     :cljs (vec (cons :<> forms))))

(defn state-wrapper [state & children]
  (condp = @state
    :loading [:div "Loading..."]
    :error [:div "Sorry, something went wrong."]
    (siblings
     (for [[i child] (map-indexed vector children)]
       (with-meta child {:key i})))))
