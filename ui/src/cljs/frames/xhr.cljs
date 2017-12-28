(ns frames.xhr
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [ui.styles :refer [style]]
            [clojure.string :as str]
            [frames.flash :as flash]
            [cognitect.transit :as transit]
            [re-frame.core :as rf]))

(rf/reg-sub
 :xhr/status
 (fn [db _]
   (get-in db [:xhr :status])))

(defn loader []
  (let [xhr-status (rf/subscribe [:xhr/status])]
    (fn []
      [:div.xhr-progress
       (style [:.xhr-progress {:position "fixed" :top "0" :width "100%" :height "1px"}
               [:.progress {:background-color "rgba(74, 144, 226, 0.35)"}]
               [:.indeterminate {:background-color "blue"}]])
       (if (= "pending" @xhr-status)
         [:div.progress [:div.indeterminate]])])))

(defn param-value [k v]
  (if (vector? v)
    (->> v (map name) (map str/trim) (str/join (str "&" (name k) "=" )))
    v))

(defn to-query [params]
  (->> (if (vector? params)
         (partition-all 2 params)
         params)
       (mapv (fn [[k v]] (str (name k) "=" (param-value k v))))
       (str/join "&")))

(defn fetch [db {:keys [uri headers params] :as opts} & [acc]]
  (let [headers (merge (or headers {})
                       {"Accept" "application/json"
                        "content-type" "application/json"
                        "Authorization" (str "Bearer " (get-in db [:auth :id_token]))})
        fetch-opts (-> (merge {:method "get" :mode "cors"} opts)
                       (dissoc :uri :headers :success :error :params)
                       (assoc :headers headers))
        fetch-opts (if (:body opts)
                     (assoc fetch-opts :body (.stringify js/JSON (clj->js (:body opts))))
                     fetch-opts)
        url (str (get-in db [:config :base-url]) uri (when params (str "?" (to-query params))))]
    (.catch
     (.then
      (js/fetch url (clj->js fetch-opts))
      (fn [resp]
        (if (not (.-ok resp))
          (do
            (.error js/console "Fetch error:" resp)
            (rf/dispatch [::flash/flash :danger (str "Request error: " (:statusText resp))])
            (rf/dispatch [::xhr-fail opts]))
          (if-not (= 204 (.-status resp))
            (.then
             (.json resp)
             (fn [data]
               (let [res {:request opts
                          :response resp
                          :data (js->clj data :keywordize-keys true)}]
                 (if acc
                   (conj acc res)
                   res))))
            {:request opts
             :response resp
             :data ""}))))
     (fn [err]
       (.error js/console "Fetch error:" err)
       (rf/dispatch [::flash/flash :danger (str "Request error: " err)])
       (rf/dispatch [::xhr-fail opts])))))


(rf/reg-fx
 :json/fetch-with-db
 (fn [[db {:keys [success error ] :as opts}]]
   (.then
    (fetch db opts)
    (fn [{:keys [response data] :as res}]
      (if (< (.-status response) 299)
        (do
          (rf/dispatch [::xhr-success opts])
          (rf/dispatch (conj success res)))
        (if error
          (rf/dispatch (conj error res))
          (rf/dispatch [::flash/flash :danger (str "Request error: " (.-status response))])))))))

(defn mk-entry-opts [entry]
  {:uri (str "/" (:resourceType entry) "/" (:id entry))
   :method "put"
   :body entry})

(rf/reg-fx
 :json/fetch-bundle-with-db
 (fn [[db {:keys [success error bundle] :as opts}]]
   (.then
    (->> (:entry bundle) (map :resource)
         (reduce (fn [acc e]
                   (.then acc (fn [r] (fetch db (mk-entry-opts e) r))))
                 (.resolve js/Promise [])))
    (fn [res]
      (if (every? #(.-ok (:response %)) res)
        (do
          (rf/dispatch [::xhr-success opts])
          (rf/dispatch (conj success res)))
        (do
          (rf/dispatch [::xhr-fail opts])
          (rf/dispatch (conj error res))))))))

(rf/reg-fx
 :json/fetch-all-with-db
 (fn [[db {:keys [success error bundle] :as opts}]]
   (.then
    (.all js/Promise
          (mapv #(fetch db %)bundle))
    (fn [res]
      (if (every? #(.-ok (:response %)) res)
        (do
          (rf/dispatch [::xhr-success opts])
          (rf/dispatch (conj success res)))
        (do
          (rf/dispatch [::xhr-fail opts])
          (rf/dispatch (conj error res))))))))

(rf/reg-event-db
 ::xhr-fail
 (fn [db [_ arg]]
   (assoc-in db [:xhr :status] "fail")))

(rf/reg-event-fx
 ::xhr-success
 (fn [{db :db} [_ arg]]
   (let [db (update-in db [:xhr :requests ] dissoc (keyword (:uri arg)))
         db (assoc-in db [:xhr :status] "success")
         db (if (empty? (get-in db [:xhr :requests ]))
              (assoc-in db [:xhr :status] "success")
              db)]
     {:db db})))

(rf/reg-event-fx
 :fetch-with-db
 (fn [{db :db} [_ arg]]
   {:db (-> db
            (assoc-in [:xhr :status] "pending")
            (assoc-in [:xhr :requests (keyword (:uri arg))] "pending"))
    :json/fetch-with-db [db arg]}))

(rf/reg-event-fx
 :xhr-fetch-with-db
 (fn [{db :db} [_ arg]]
   {:db (-> db
            (assoc-in [:xhr :status] "pending")
            (assoc-in [:xhr :requests (keyword (:uri arg))] "pending"))
    :xhr/fetch-with-db [db arg]}))

(rf/reg-event-fx
 :fetch-bundle-with-db
 (fn [{db :db} [_ arg]]
   {:json/fetch-bundle-with-db [db arg]}))

(rf/reg-event-fx
 :fetch-all-with-db
 (fn [{db :db} [_ arg]]
   {:db (-> db
            (assoc-in [:xhr :status] "pending")
            (assoc-in [:xhr :requests (keyword (or (:uri arg) "/"))] "pending"))
    :json/fetch-all-with-db [db arg]}))


(rf/reg-fx
 :xhr/fetch
 #(rf/dispatch [:xhr-fetch-with-db %]))
(rf/reg-fx
 :json/fetch
 #(rf/dispatch [:fetch-with-db %]))
(rf/reg-fx
 :json/fetch-all
 #(rf/dispatch [:fetch-all-with-db %]))
(rf/reg-fx
 :json/bundle
 #(rf/dispatch [:fetch-bundle-with-db %]))
