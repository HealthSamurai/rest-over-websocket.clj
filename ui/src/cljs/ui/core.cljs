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
            [ui.routes :as routes]
            [ui.layout :as layout]
            [ui.page.index]
            [cljsjs.moment]
            [cljsjs.moment.locale.ru]
            [cljsjs.medium-editor]
            [re-frisk.core :refer [enable-re-frisk!]]))

(.locale js/moment "ru")

(def default-cfg
  {:base-url "http://localhost:8080"
   :client_id "local"})

(rf/reg-event-fx
 :core/initialize
 [(rf/inject-cofx ::cookies/get :auth)
  (rf/inject-cofx ::storage/get :account)
  (rf/inject-cofx :window-location)
  (rf/inject-cofx ::openid/jwt :auth)]
 (fn [{jwt :jwt
       {auth :auth account :account :as cook} :cookie
       {account :account} :storage
       {qs :query-string hash :hash url :url :as loc} :location
       {config :config :as db} :db
       :as cofx} [_ opts]]
   (let [config (merge default-cfg config opts qs)]
       {:dispatch [:route-map/init routes/routes]
        :db {:route-map/current-route {:match :loading}}})))

(defn page []
  (let [route (rf/subscribe [:route-map/current-route])]
    (fn []
      (let [match (:match @route)
            page-comp (get @pages/pages (:match @route))]
        [layout/page-layout
         (if page-comp
           page-comp
           (fn [] [:h1 (str "404 Страница не найдена")]))]))))

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
