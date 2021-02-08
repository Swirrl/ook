;; This is here for devcards because shadow-cljs does not support :foreign-libs
;; and marked is not bundled with devcards
;; see https://github.com/bhauman/devcards/issues/168#issuecomment-640257949

(ns cljsjs.marked
  (:require ["marked" :as marked]))

(js/goog.exportSymbol "marked" marked)
(js/goog.exportSymbol "DevcardsMarked" marked)
