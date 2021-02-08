;; This is here for devcards because shadow-cljs does not support :foreign-libs
;; and highlight.js is not bundled with devcards
;; see https://github.com/bhauman/devcards/issues/168#issuecomment-640257949

(ns cljsjs.highlight
  (:require ["highlight.js" :as highlight]))

(js/goog.exportSymbol "syntax-highlighter" highlight)
(js/goog.exportSymbol "DevcardsSyntaxHighlighter" highlight)
