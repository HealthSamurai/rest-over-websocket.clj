(ns ui.page.index
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [re-frame.core :as rf]
            [cljs.core.async :as a :refer [<! >!]]
            [haslett.client :as ws]
            [haslett.format :as fmt]
            [frames.pages :refer [reg-page]]))

(rf/reg-event-fx
 :connect
 (fn [{db :db} _]
   {:websocket-connet {}}))

(defonce websocket (atom nil))

(rf/reg-fx
 :websocket-connet
 (fn [_]
   (go
     (let [stream (<! (ws/connect "ws://localhost:8080/$conn"))]
       (reset! websocket stream)
       (while (not (nil? @websocket))
         (let [message (<! (:source stream))]
           (if (nil? message)
             (let [close-status (<! (:close-status stream))]
               (js/console.log close-status)
               (reset! websocket nil))
             (js/console.log message))))))))

(defn index [params]
  [:div
   [:h2 "hello world"]
   [:button {:on-click #(rf/dispatch [:connect])} "connect"]])

(reg-page :index/index index)


(comment

  (go (>! (:sink @websocket) "Hello World"))

)
