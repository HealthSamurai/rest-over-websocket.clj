(ns frames.flash
  (:require [ui.styles :refer [style]]
            [re-frame.core :as rf]))

(rf/reg-event-db
 ::flash
 (fn [db [_ status message]]
   (let [id (keyword (str (.getTime (js/Date.))))]
     (js/setTimeout #(rf/dispatch [::remove-flash id]) 3000)
     (assoc-in db [:flash id] {:st status :msg message}))))

(rf/reg-event-fx
 ::remove-flash
 (fn [{db :db} [_ id]]
   {:db (update db :flash dissoc id)}))

(rf/reg-fx
 :flash/flash
 (fn [{:keys [status message]}]
   (rf/dispatch [::flash status message])))

(rf/reg-sub
 ::flashes
 (fn [db _]
   (:flash db)))

(def styles
  (style
   [:.flashes {:position "fixed" :top "20px" :right "20px" :max-width "500px"}]))

(defn flash-msg [id f]
  [:div.toast.flash-msg.alert.alert-dismissible {:class (str "alert-" (name (:st f)))}
   #_[:button.close 
    [:span {:on-click #(rf/dispatch [::remove-flash id])} "×"] ]
   (:msg f)])

(defn flashes []
  (let [flashes (rf/subscribe [::flashes])]
    (fn []
      (into [:div.flashes]
            (reduce-kv (fn [acc k f]
                         (conj acc (flash-msg k f)))
                       [] @flashes)))))
