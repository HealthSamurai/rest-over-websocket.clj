(ns ui.socket
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
 :ws/connect
 (fn [opts]
   (go
     (let [l (.-location js/window)
           url (if (= "localhost" (.-hostname l))
                 "ws://localhost:8080/$conn"
                 "wss://row-back.health-samurai.io/$conn")
           stream (<! (ws/connect url))]
       (reset! websocket stream)
       (.log js/console "Connected to " url (:socket stream))
       (.log js/console "Dispatch" (:dispatch opts))
       (rf/dispatch (:dispatch opts))
       (while (not (nil? @websocket))
         (let [message (<! (:source stream))]
           (if (nil? message)
             (let [close-status (<! (:close-status stream))]
               (js/console.log close-status)
               (reset! websocket nil))
             (let [jm (.parse js/JSON message)
                   m (js->clj jm :keywordize-keys true)
                   ev (keyword (get-in m [:request :success]))]
               (.groupCollapsed js/console "<"
                       (get-in m [:request :request-method])
                       (get-in m [:request :uri])
                       (str "(dispatch :" (get-in m [:request :success]) ")"))
               (.log js/console jm)
               (.log js/console (pr-str m))
               (.groupEnd js/console)
               (rf/dispatch [ev m])))))))))

(rf/reg-fx
 :http/xhr
 (fn [opts]
   (doseq [req (if (vector? opts) opts [opts])]
     (.groupCollapsed js/console (:request-method req) (:uri req))
     (.log js/console (clj->js req))
     (.groupEnd js/console))

   (.send (:socket @websocket)
          (.stringify js/JSON (clj->js opts)))))
