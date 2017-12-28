(ns frames.redirect
  (:require [re-frame.core :as rf]
            [clojure.string :as str]))

(defn page-redirect [url]
  (set! (.-href (.-location js/window)) url))

(rf/reg-fx
 ::site-redirect
 (fn [opts]
   (page-redirect (str (:uri opts)
                       (when-let [params (:params opts)]
                         (->> params
                              (map (fn [[k v]] (str (name k) "=" (js/encodeURIComponent v))))
                              (str/join "&")
                              (str "?")))))))

(rf/reg-fx
 ::page-redirect
 (fn [url]
   (set! js/location.hash url)))

(rf/reg-fx
 :page-redirect
 (fn [url]
   (set! js/location.hash url)))

(rf/reg-fx
 :redirect/redirect
 (fn [{:keys [url]}]
   (set! js/location.hash url)))

(rf/reg-event-fx
 ::page-redirect
 (fn [coef [_ url]]
   {::page-redirect url}))

(rf/reg-event-fx
 :redirect/redirect
 (fn [coef [_ url]]
   {::page-redirect url}))

(rf/reg-fx
 ::reload
 (fn [_]
   (js/location.reload)))
