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
 :ws/connet
 (fn [_]
   (go
     (let [stream (<! (ws/connect "ws://localhost:8080/$conn"))]
       (reset! websocket stream)
       (.log js/console "Connected" stream)
       (while (not (nil? @websocket))
         (let [message (<! (:source stream))]
           (if (nil? message)
             (let [close-status (<! (:close-status stream))]
               (js/console.log close-status)
               (reset! websocket nil))
             (let [m (js->clj (.parse js/JSON message)
                              :keywordize-keys true)
                   ev (keyword (get-in m [:request :success]))]
               (.log js/console "<=" (.parse js/JSON message))
               (println "<-"
                        (get-in m [:request :request-method])
                        (get-in m [:request :uri])
                        " dispatch " ev
                        (:body m))
               (rf/dispatch [ev m])))))))))

(rf/reg-fx
 :http/xhr
 (fn [opts]
   (doseq [req (if (vector? opts) opts [opts])]
     (println "->" (:request-method req) (:uri req) req))
   
   (.send (:socket @websocket)
          (.stringify js/JSON (clj->js opts)))))
