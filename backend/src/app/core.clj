(ns app.core
  (:require [org.httpkit.server :as server]
            [cheshire.core :as json]
            [clj-yaml.core :as yaml]
            [route-map.core :as routing]
            ))

(defn dispatch [req]
  {:status 200
   :body "hello"})

(def routes
  {:GET          {:action :index}
   "$conn"       {:GET {:action :connection}}
   "admin"       {"users" {:GET {:action :get-users}}}})

(defonce connections (atom #{}))

(defn dispatch-socket-request [req]
  {:body {:some "data"
          :request req}})

(defn connection-handler [req]
  (server/with-channel req ch
    (println "incomming conn" ch)
    (server/send! ch "hello")
    (swap! connections conj ch)
    (server/on-close
     ch (fn [status]
          (swap! connections disj ch)
          (println "channel closed: " status)))
    (server/on-receive
     ch (fn [data]
          (server/send! ch (json/generate-string (dispatch-socket-request (json/parse-string data))))))))

(server/send!
 (first @connections)
 "hello")


(def fns-map {:index (fn [_] {:body "<html></html>"})
              :connection #'connection-handler})

(defn format-mw [f]
  (fn [req]
    (let [resp (f req)]
      (if (:body resp)
        ;; (update resp :body json/generate-string)
        (update resp :body yaml/generate-string)
        resp))))

(defn resolve-route [f]
  (fn [{uri :uri meth :request-method :as req}]
    (if-let [m (routing/match [meth uri] #'routes)]
      (f (assoc req :current-route (:match m) :route-params (:params m)))
      {:status 404 :body {:message (str uri " is not found")}})))

(defn *dispatch [{route :current-route :as req}]
  (if-let [h (get fns-map (:action route))]
    (h req)
    {:status 404 :body {:message (str "No implementation for " route)}}))

(def dispatch (-> *dispatch resolve-route ))
;; format-mw

(defn start []
  (server/run-server #'dispatch {:port 8080}))

(comment
  (def srv (start))
  ;; stop it
  (srv)
  )
