(ns app.core
  (:require [org.httpkit.server :as server]))

(defn dispatch [req]
  {:status 200
   :body "hello"})

(defn start []
  (server/run-server #'dispatch {:port 8889}))

(comment
  (def srv (start))
  ;; stop it
  (srv))

(dispatch {})

;; json middle ware

(require '[cheshire.core :as json])
(require '[clj-yaml.core :as yaml])

(defn format-mw [f]
  (fn [req]
    (let [resp (f req)]
      (if (:body resp)
        ;; (update resp :body json/generate-string)
        (update resp :body yaml/generate-string)
        resp))))

(def mystack (-> identity format-mw ))

(mystack {:body {:message "ok"}})

(defn index [req]
  {:status 200
   :body {:message "ok"}})

(def dispatch (-> #'index format-mw))

(require '[route-map.core :as routing])

(def routes
  {:GET         {:action :index}
   "admin"       {"users" {:GET {:action :get-users}
                           :auth [:admin]}}
   [:table_name] {:GET {:action :select}}})

(-> (routing/match [:GET "/"] routes)
    :match)

(-> (routing/match [:GET "/admin/users"] routes)
    :match)

(-> (routing/match [:GET "/admin/ups"] routes)
    :match)

(-> (routing/match [:GET "/tables"] routes)
    :params)

(defn resolve-route [f]
  (fn [{uri :uri meth :request-method :as req}]
    (if-let [m (routing/match [meth uri] routes)]
      (f (assoc req :current-route (:match m) :route-params (:params m)))
      {:status 404 :body {:message (str uri " is not found")}})))

((-> identity resolve-route) {:uri "/" :request-method "get"})


(defn select-handler [{{tbl :table_name } :route-params :as req}]
  {:body {:select_from tbl}})

(dispatch {:uri "/sometable" :request-method "get"})

(def fns-map {:index #'index
              :select #'select-handler})

(defn *dispatch [{route :current-route :as req}]
  (if-let [h (get fns-map (:action route))]
    (h req)
    {:status 404 :body {:message (str "No implementation for " route)}}))

(def dispatch (-> *dispatch resolve-route format-mw))

(dispatch {:uri "/" :request-method "get"})

