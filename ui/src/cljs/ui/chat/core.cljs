(ns ui.chat.core
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [ui.routes :refer [href]]
            [frames.pages :refer [reg-page]]
            [ui.chat.model]
            [ui.styles :as styles :refer [style]]))

(def index-styles
  [:div.app
   {:position "absolute"
    :top "50px"
    :left "150px"
    :bottom "0px"
    :right "150px"}
   [:.content {:position "absolute"
               :padding "0px"
               :background-color "rgba(244,244,244,0.5)"
               :top "50px"
               :left "250px"
               ;; :border "1px solid #dddd"
               :bottom "50px"
               :right "50px"}]
   [:.new {:padding "20px"}
    [:input {:width "100%"
             :padding "10px"}]]

   [:.header {:padding "15px"
              :background-color "rgba(255,255,255,0.6)"}]
   [:.messages
    [:.messages-content
     {:overflow-y "auto"
      :position "absolute"
      :top "50px"
      :left "0px"
      ;; :background-color "#f1f1f1"
      :padding "10px"
      :right "0px"
      :bottom "50px"}]
    [:.message {:background-color "white"
                :border-radius "4px"
                :margin-bottom "10px"
                :width "auto"
                :padding "10px"}
     [:.author {:margin-right "10px"
                :color "gray"}]
     [:.ava {:width "30px" :height "30px" :border-radius "50%" :margin-right "5px"}]
     ]
    [:.input {:position "absolute"
              :bottom 0
              :left 0
              :right 0
              :overflow "hidden"
              :border-top "1px solid #aaa"
              :height "60px"}
     [:textarea {:width "99%"
                 :padding "15px"
                 :background-color "rgba(255,255,255,0.8)"
                 :font-size "16px"
                 :border "none"
                 :height "56px"}]]]
   [:.rooms
    {:position "absolute"
     :background-color "white"
     :border-right "1px solid #dddd"
     :top "50px"
     :left "50px"
     :bottom "50px"
     :margin "0px"
     :width "200px"}
    [:.room {:display "block"
             :color "#555"
             :border-bottom "1px solid #f1f1f1"
             :border-top "1px solid #f1f1f1"
             :margin-top "-1px"
             :text-decoration "none"
             :padding "15px"}
     [:&.active {:background-color "#777"
                 :color "white"}]]]])

(defn rooms [params]
  (let [chat (rf/subscribe [:chat])]
    (fn [params]
      [:div.rooms
       [:div.header
        [:b "Chats: "]
        [:a.pull-right {:href (href "rooms" "new")} "new room"]]
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

(defn message-list []
  (let [messages (rf/subscribe [:messages])]
    [:div.messages-content
     (for [m @messages]
       [:div.message {:key (:id m)
                      :ref #(and % (.scrollIntoView % {"behaviour" "smooth"}))}
        [:span.author
         [:img.ava {:src (get-in m [:author :picture])}]
         (get-in m [:author :name])]
        ;; (pr-str (:author m))
        (:text m)])]))

(defn messages [{rid :id}]
  (rf/dispatch [:init-messages rid])
  (let [input (rf/subscribe [:value [:chat rid :input]])
        on-key-down (fn [ev] (when (= 13 (.-which ev))
                               (rf/dispatch [:send-message rid @input])))
        on-change #(rf/dispatch [:on-change [:chat rid :input] (.. % -target -value)])]
    (fn [params]
      [:div.content.messages
       [:div.header [:b "messages"]]
       [message-list]
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

(defn new [{rid :id}]
  (rf/dispatch [:init-chat])
  (let [chat (rf/subscribe [:chat])
        form-path [:new :chat]
        value (rf/subscribe [:value form-path])
        submit #(rf/dispatch [:new-chat @value])
        on-change #(rf/dispatch [:on-change form-path (.. % -target -value)])]
    (fn [params]
      [:div.app
       [style index-styles]
       [rooms params]
       [:div.content.new
        [:div.header [:b "New chat"]]
        [:hr]
        [:input.name {:value @value :on-change on-change}]
        [:br]
        [:br]
        [:button.btn.btn-success {:on-click submit} "Create!"]]])))

(reg-page :index/index index)
(reg-page :rooms/show show)
(reg-page :rooms/new new)
