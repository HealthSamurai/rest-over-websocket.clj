(ns ui.core
  (:require [frames.routing]
            [frames.xhr :as xhr]
            [frames.flash :as flash]
            [frames.modal :as modal]
            [frames.cookies :as cookies]
            [frames.storage :as storage]
            [frames.openid :as openid]
            [frames.redirect :as redirect]
            [frames.window-location :as location]
            [frames.pages :as pages]
            [reagent.core :as reagent]
            [re-frame.core :as rf]
            [clojure.string :as str]
            [ui.styles :as styles]
            ;; [ui.page.index :as index]
            [ui.routes :as routes]
            [ui.chat.core]
            [cljsjs.moment]
            [cljsjs.moment.locale.ru]
            [cljsjs.medium-editor]
            [ui.socket :as socket]
            [re-frisk.core :refer [enable-re-frisk!]]))

;; (.locale js/moment "ru")

(def default-cfg
  {:base-url "http://localhost:8080"
   :client_id "local"})

;; (rf/reg-event-fx
;;  :core/initialize
;;  (fn [{{config :config :as db} :db :as cofx} [_ opts]]
;;    (let [config (merge default-cfg config opts)]
;;        {:dispatch [:route-map/init routes/routes]
;;         :db {:route-map/current-route {:match :loading}}
;;         :ws/connet {}})))

;; (def open-id-keys
;;   {:client-id "412051595354-hfbo8obml5gadn2taruqbs76sivt7mbt.apps.googleusercontent.com"
;;    :uri "https://accounts.google.com/o/oauth2/v2/auth"})
(def open-id-keys
  {:client-id "646067746089-6ujhvnv1bi8qvd7due8hdp3ob9qtcumv.apps.googleusercontent.com"
   :uri "https://accounts.google.com/o/oauth2/v2/auth"})

(rf/reg-event-fx
 :core/initialize
 [(rf/inject-cofx ::cookies/get :auth)
  (rf/inject-cofx :window-location)
  (rf/inject-cofx ::openid/jwt :auth)]
 (fn [{jwt :jwt {auth :auth} :cookie
       {qs :query-string hash :hash url :url :as loc} :location
       :as cofx} _]
   (if (and (nil? jwt) (nil? auth))
     {::redirect/site-redirect {:uri (:uri open-id-keys)
                                :params {:redirect_uri (first (str/split (.. js/window -location -href) #"#"))
                                         :client_id (:client-id open-id-keys)
                                         :scope "openid profile email"
                                         :nonce "ups"
                                         :response_type "id_token"}}}
     {::cookies/set {:key :auth :value (or jwt auth)}
      :dispatch [:route-map/init routes/routes]
      ::redirect/page-redirect "/"
      :ws/connet {}
      :db       (assoc (:db cofx) :auth (or jwt auth))})))


(defn layout [content]
  [:div content])

(defn page []
  (let [route (rf/subscribe [:route-map/current-route])]
    (fn []
      (let [match (:match @route)
            page-comp (get @pages/pages (:match @route))]
        [layout
         (if page-comp
           [page-comp (:params @route)]
           [:h1 (str "404 Страница не найдена")])]))))

(defn root-component []
  [:div#root
   flash/styles
   styles/common-styles
   [page]
   [flash/flashes]
   [modal/modal]
   [xhr/loader]])

(defn mount-root []
  (reagent/render [root-component] (.getElementById js/document "app")))

(defn init! [& [opts]]
  (rf/dispatch-sync [:core/initialize opts])
  (mount-root))


