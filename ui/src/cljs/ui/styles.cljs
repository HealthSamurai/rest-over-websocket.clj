(ns ui.styles
  (:require [garden.core :as garden]
            [garden.units :as units]))

(defn style [css]
  [:style (garden/css css)])




(def *colors
  {:active "gray"
   :passive "lightgray"
   :second "blue"
   :inactive "darkgray"})


(defn colors [nm]
  (get *colors nm))

(def panel
  [:.panel
   {:background-color (colors :passive)
    ;:border (str "1px solid " (colors :active))
    :box-shadow (str "0 0 10px 0 " (colors :active))}
   [:&.hoverable
    [:&:hover
     {:background-color "green"}]]])

(def common-styles
  (style [panel]))
