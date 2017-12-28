(ns ui.chat.core
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [ui.routes :refer [href]]
            [frames.pages :refer [reg-page]]
            [ui.chat.model]
            [ui.styles :as styles :refer [style]]))

(def index-styles
  [:div.app
   [:.content {:position "absolute"
               :padding "20px"
               :background-color "white"
               :top "50px"
               :left "250px"
               :border "1px solid #dddd"
               :bottom "50px"
               :right "50px"}]
   [:.messages
    [:.messages-content
     {:overflow-y "auto"
      :position "absolute"
      :top "50px"
      :left "0px"
      :background-color "#f1f1f1"
      :padding "10px"
      :right "0px"
      :bottom "50px"}]
    [:.message {:background-color "white"
                :border-radius "4px"
                :margin-bottom "10px"
                :width "auto"
                :padding "10px"}]
    [:.input {:position "absolute"
              :bottom 0
              :left 0
              :right 0
              :overflow "hidden"
              :border-top "1px solid #aaa"
              :height "60px"}
     [:textarea {:width "99%"
                 :font-size "16px"
                 :border "none"
                 :height "56px"}]]]
   [:.rooms
    {:position "absolute"
     :background-color "white"
     :border "1px solid #dddd"
     :top "50px"
     :left "50px"
     :bottom "50px"
     :margin "0px"
     :width "200px"}
    [:.header {:padding "10px"}]
    [:.room {:display "block"
             :color "#555"
             :text-decoration "none"
             :padding "10px 10px"}
     [:&.active {:background-color "#777"
                 :color "white"}]]]])

(defn rooms [params]
  (let [chat (rf/subscribe [:chat])]
    (fn [params]
      [:div.rooms
       [:div.header
        [:b "ROOMS:"]
        [:a {:href (href "rooms" "new")} "New room"]]
       (for [{:keys [id title]} (:rooms @chat)]
         [:a.room {:key id
                   :class (when (= (str id) (:id params)) "active")
                   :href (href "rooms" id)}
          [:div.name title]])
       ])))

(defn index [params]
  (rf/dispatch [:init-chat])
  (let [chat (rf/subscribe [:chat])]
    (fn []
      [:div.app
       [style index-styles]
       [rooms params]])))

(defn messages [{rid :id}]
  (rf/dispatch [:init-messages rid])
  (let [messages (rf/subscribe [:messages])
        input (rf/subscribe [:value [:chat rid :input]])
        on-key-down (fn [ev] (when (= 13 (.-which ev))
                               (rf/dispatch [:send-message rid @input])))
        on-change #(rf/dispatch [:on-change [:chat rid :input] (.. % -target -value)])]
    (fn [params]
      [:div.content.messages
       [:div.header "messages"]
       [:div.messages-content
        (for [m @messages]
          [:div.message {:key (:id m)}
           (:message m)])]
       [:div.input
        [:textarea {:value @input
                    :on-key-down on-key-down
                    :on-change on-change}]]])))

(defn show [params]
  (rf/dispatch [:init-chat])
  (let [chat (rf/subscribe [:chat])]
    (fn [params]
      [:div.app
       [style index-styles]
       [rooms params]
       ^{:key (:id params)}[messages params]])))

(reg-page :index/index index)
(reg-page :rooms/show show)
