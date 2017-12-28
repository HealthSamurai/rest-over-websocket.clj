(ns ui.layout
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require
   [reagent.core :as r]
   [re-frame.core :as rf]
   [clojure.string :as str]))

(defn page-layout [content]
    [:div [content]])
