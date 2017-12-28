(ns ui.page.index
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as rf]
            [cljs.core.async :as a :refer [<! >!]]
            [haslett.client :as ws]
            [haslett.format :as fmt]
            [ui.routes :refer [href]]
            [frames.pages :refer [reg-page]]
            [ui.styles :as styles :refer [style]]))


(rf/reg-event-fx
 :rooms-load
 (fn [{db :db} _]
   {:http/xhr {:uri "/rooms"
               :request-method "get"
               :success :rooms-success}}))

(rf/reg-event-db
 :rooms-success
 (fn [db [_ data]]
   (.log js/console "rooms-loaded" data)
   (assoc db :rooms (:body data))))

(rf/reg-sub
 :rooms
 (fn [db] (:rooms db)))


(def styles
  (style
   [:.rooms
    {:margin "15px"}
    [:.rooms-list
     {:display "flex"
      :margin-left "20px"
      :margin-right "20px"}
     [:.room
      {:margin "20px"
       :margin-left "0px"
       :height "100px"
       :width "100px"
       :background-color "gray"}]]]))

(def mock-rooms [{:id :bla
                  :name "First room"}
                 {:id :ble
                  :name "Second room"}])

(rf/reg-event-fx
 :go-to-room
 (fn [{:keys [db]} [_ rid]]
   {:db (assoc db :current-room rid)
    :dispatch [:init-room rid]}))

#_(rf/reg-sub-raw
 :get
 (fn [db [_ key]]
   #_(r/reaction )))

(defn index [params]
  (rf/dispatch [:rooms-load])
  (let [rooms mock-rooms #_(rf/subscribe [:rooms])
        messages (rf/subscribe [:messages])
        room-info (rf/subscribe [:room])
        ;current-room (rf/subscribe [:get :current-room])
        ]
    (fn []
      [:div.wrapper
       [:div.container
        [:div.left
         [:div.top
          [:input {:type "text"}]
          [:a.search {:href "javascript:;"}]]
         [:ul.people
          [:li.person
           {:data-chat "person1"}
           [:img
            {:alt "", :src "https://s13.postimg.org/ih41k9tqr/img1.jpg"}]
           [:span.name "Thomas Bangalter"]
           [:span.time "2:09 PM"]
           [:span.preview "I was wondering..."]]
          [:li.person
           {:data-chat "person2"}
           [:img
            {:alt "", :src "https://s3.postimg.org/yf86x7z1r/img2.jpg"}]
           [:span.name "Dog Woofson"]
           [:span.time "1:44 PM"]
           [:span.preview "I've forgotten how it felt before"]]
          [:li.person
           {:data-chat "person3"}
           [:img
            {:alt "", :src "https://s3.postimg.org/h9q4sm433/img3.jpg"}]
           [:span.name "Louis CK"]
           [:span.time "2:09 PM"]
           [:span.preview "But we’re probably gonna need a new carpet."]]
          [:li.person
           {:data-chat "person4"}
           [:img
            {:alt "", :src "https://s3.postimg.org/quect8isv/img4.jpg"}]
           [:span.name "Bo Jackson"]
           [:span.time "2:09 PM"]
           [:span.preview "It’s not that bad..."]]
          [:li.person
           {:data-chat "person5"}
           [:img
            {:alt "", :src "https://s16.postimg.org/ete1l89z5/img5.jpg"}]
           [:span.name "Michael Jordan"]
           [:span.time "2:09 PM"]
           [:span.preview
            "Wasup for the third time like is \nyou bling bitch"]]
          [:li.person
           {:data-chat "person6"}
           [:img
            {:alt "", :src "https://s30.postimg.org/kwi7e42rh/img6.jpg"}]
           [:span.name "Drake"]
           [:span.time "2:09 PM"]
           [:span.preview "howdoyoudoaspace"]]]]
        [:div.right
         [:div.top [:span "To: " [:span.name "Dog Woofson"]]]
         [:div.chat
          {:data-chat "person1"}
          [:div.conversation-start [:span "Today, 6:48 AM"]]
          [:div.bubble.you
           "\n                    Hello,\n                "]
          [:div.bubble.you
           "\n                    it's me.\n                "]
          [:div.bubble.you
           "\n                    I was wondering...\n                "]]
         [:div.chat
          {:data-chat "person2"}
          [:div.conversation-start [:span "Today, 5:38 PM"]]
          [:div.bubble.you
           "\n                    Hello, can you hear me?\n                "]
          [:div.bubble.you
           "\n                    I'm in California dreaming\n                "]
          [:div.bubble.me
           "\n                    ... about who we used to be.\n                "]
          [:div.bubble.me
           "\n                    Are you serious?\n                "]
          [:div.bubble.you
           "\n                    When we were younger and free...\n                "]
          [:div.bubble.you
           "\n                    I've forgotten how it felt before\n                "]]
         [:div.chat
          {:data-chat "person3"}
          [:div.conversation-start [:span "Today, 3:38 AM"]]
          [:div.bubble.you
           "\n                    Hey human!\n                "]
          [:div.bubble.you
           "\n                    Umm... Someone took a shit in the hallway.\n                "]
          [:div.bubble.me
           "\n                    ... what.\n                "]
          [:div.bubble.me
           "\n                    Are you serious?\n                "]
          [:div.bubble.you
           "\n                    I mean...\n                "]
          [:div.bubble.you
           "\n                    It’s not that bad...\n                "]
          [:div.bubble.you
           "\n                    But we’re probably gonna need a new carpet.\n                "]]
         [:div.chat
          {:data-chat "person4"}
          [:div.conversation-start [:span "Yesterday, 4:20 PM"]]
          [:div.bubble.me
           "\n                    Hey human!\n                "]
          [:div.bubble.me
           "\n                    Umm... Someone took a shit in the hallway.\n                "]
          [:div.bubble.you
           "\n                    ... what.\n                "]
          [:div.bubble.you
           "\n                    Are you serious?\n                "]
          [:div.bubble.me
           "\n                    I mean...\n                "]
          [:div.bubble.me
           "\n                    It’s not that bad...\n                "]]
         [:div.chat
          {:data-chat "person5"}
          [:div.conversation-start [:span "Today, 6:28 AM"]]
          [:div.bubble.you
           "\n                    Wasup\n                "]
          [:div.bubble.you
           "\n                    Wasup\n                "]
          [:div.bubble.you
           "\n                    Wasup for the third time like is "
           [:br]
           "you blind bitch\n                "]]
         [:div.chat
          {:data-chat "person6"}
          [:div.conversation-start [:span "Monday, 1:27 PM"]]
          [:div.bubble.you
           "\n                    So, how's your new phone?\n                "]
          [:div.bubble.you
           "\n                    You finally have a smartphone :D\n                "]
          [:div.bubble.me
           "\n                    Drake?\n                "]
          [:div.bubble.me
           "\n                    Why aren't you answering?\n                "]
          [:div.bubble.you
           "\n                    howdoyoudoaspace\n                "]]
         [:div.write
          [:a.write-link.attach {:href "javascript:;"}]
          [:input {:type "text"}]
          [:a.write-link.smiley {:href "javascript:;"}]
          [:a.write-link.send {:href "javascript:;"}]]]]]

      #_[:div.app
       [:div.panel.rooms
        [:div.rooms-list
         (for [{:keys [id name]} rooms]
           [:div.room {:class (when (= (:current-room ) id) "current")
                       :on-click #(dispatch [:go-to-room id])}
            [:div.name name]])]
        [:div.chat
         (for [msg @messages]
           [:div.message
            (pr-str msg)])]]])))

(def user-id "123-321-231")

(rf/reg-event-fx
 :init-room
 (fn [{db :db} [_ rid]]
   {:http/xhr [{:uri "register"
                :request-method "post"
                :body {:user-id user-id
                       :name "Ilya Beda"}}
               {:uri (str "/rooms/" rid)
                :request-method "get"
                :success :room-success}
               {:uri (str "/rooms/" rid "/messages")
                :params {:room_id rid}
                :request-method "get"
                :success :room-messages}
               {:uri (str "/rooms/" rid "/messages")
                :request-method "sub"
                :body {:user-id user-id}
                :success :room-messages-change}]}))

(rf/reg-event-fx
 :add-message
 (fn [_ [_ rid]]
   {:http/xhr {:uri (str "/rooms/" rid )
                :request-method "post"
                :body {:user-id user-id
                       :text "hello"}}}))


(rf/reg-event-db
 :room-success
 (fn [db [_ {room :body}]]
   (assoc db :room room)))

(rf/reg-event-db
 :room-success
 (fn [db [_ {{:keys [id] :as info} :body}]]
   (js/console.log "Room data:" info)
   (assoc-in db [:room id] info)))

(rf/reg-event-db
 :room-messages
 (fn [db [_ {msgs :body}]]
   (assoc db :messages msgs)))

(rf/reg-event-db
 :room-messages-change
 (fn [db [_ {msgs :body}]]
   (update db :messages concat msgs)))

(rf/reg-sub
 :messages
 (fn [db  _] (:messages db)))

(rf/reg-sub
 :room
 (fn [db [_ rid]] (get-in db [:room rid])))

(defn show [{room-id :id}]
  (rf/dispatch [:init-room room-id])
  (let [messages (rf/subscribe [:messages])]
    (fn []
      [:div
       [:button {:on-click #(rf/dispatch [:add-message room-id])} "add message"]
       [:div
        (pr-str @messages)]])))

(reg-page :index/index index)
(reg-page :rooms/show show)

(comment

  (go (>! (:sink @websocket) "Hello World"))

  )
