(ns ook.ui.icons)

(defn- icon [& elements]
  [:svg {:xmlns "http://www.w3.org/2000/svg" :width 16 :height 16 :fill "black" :viewBox "0 0 16 16"}
   elements])

(def down-path "M7.247 11.14 2.451 5.658C1.885 5.013 2.345 4 3.204 4h9.592a1 1 0 0 1 .753 1.659l-4.796 5.48a1 1 0 0 1-1.506 0z")
(def down
  (icon
   ^{:key "path"}[:path {:d down-path}]))

(def up-path "m12.14 8.753-5.482 4.796c-.646.566-1.658.106-1.658-.753V3.204a1 1 0 0 1 1.659-.753l5.48 4.796a1 1 0 0 1 0 1.506z")
(def up
  (icon
   ^{:key "path"}[:path {:d up-path}]))
