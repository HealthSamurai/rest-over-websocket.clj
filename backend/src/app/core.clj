(ns app.core
  (:require [org.httpkit.server :as server]
            [cheshire.core :as json]
            [clj-yaml.core :as yaml]
            [route-map.core :as routing]
            ))

(defn dispatch [req]
  {:status 200
   :body "hello"})

(defn get-rooms [_]
  {:body [{:id "clojure" :title "Clojure"}
          {:id "haskel" :title "Haskel"}]})

(def routes
  {:GET          {:action :index}
   "$conn"       {:GET {:action :connection}}
   "register" {:POST {:action :register}}
   "rooms"    {:GET  {:action :rooms}
               [:id] {:GET {:action :room}

                      :POST {:action :add-message}}}})
(defn index [_] {:body "<html></html>"})

(def fns-map {:index index
              :rooms #'get-rooms
              :connection #'connection-handler})

(defonce connections (atom #{}))

(declare *dispatch)

(defn dispatch-socket-request [req]
  (println "dispatch" req)
  (*dispatch req))

(defn connection-handler [req]
  (server/with-channel req ch
    (server/send! ch "hello")
    (swap! connections conj ch)
    (server/on-close
     ch (fn [status]
          (swap! connections disj ch)
          (println "channel closed: " status)))
    (server/on-receive
     ch (fn [data]
          (server/send! ch
                        (json/generate-string
                         (dispatch-socket-request (json/parse-string data keyword))))))))



(defn format-mw [f]
  (fn [req]
    (let [resp (f req)]
      (if (and (:body resp) (coll? (:body resp)))
        (update resp :body json/generate-string)
        ;;(update resp :body yaml/generate-string)
        resp))))

(defn *dispatch [{uri :uri meth :request-method :as req}]
  (if-let [m (routing/match [meth uri] #'routes)]
    (let [req (assoc req :current-route (:match m) :route-params (:params m))]
      (if-let [h (get fns-map (:action (:match m)))]
        (h req)
        {:status 404 :body {:message (str "No implementation for " (:match m))}}))
    {:status 404 :body {:message (str uri " is not found")}}))

(def dispatch (-> *dispatch format-mw))
;; format-mw

(defn start []
  (server/run-server #'dispatch {:port 8080}))

(comment
  (def srv (start))
  ;; stop it
  (srv)
  )
