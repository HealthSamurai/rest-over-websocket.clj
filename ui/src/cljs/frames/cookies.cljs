(ns frames.cookies
  (:refer-clojure :exclude [get set!])
  (:require [goog.net.cookies :as gcookies]
            [re-frame.core :as rf]))

(defn remove! [k]
  (.remove goog.net.cookies (name k)))

(defn keywordize [x]
  (js->clj x :keywordize-keys true))

(defn get-cookie [k]
  (try (->> k name
           (.get goog.net.cookies)
           js/decodeURIComponent
           (.parse js/JSON)
           (keywordize))
       (catch js/Object e (do (remove! k) nil))))

(defn set-cookie [k v]
  (->> v
       clj->js
       (.stringify js/JSON)
       js/encodeURIComponent
       (.set goog.net.cookies (name k) )))

(rf/reg-cofx
 ::get
 (fn [coeffects key]
   (assoc-in coeffects [:cookie key] (get-cookie key))))

(rf/reg-fx
 ::set
 (fn [{k :key v :value :as opts}]
   (set-cookie k v)))

(rf/reg-fx
 :cookie/set
 (fn [{k :key v :value :as opts}]
   (set-cookie k v)))

(rf/reg-fx
 ::remove
 (fn [{k :key ks :keys}]
   (when k
     (.remove goog.net.cookies (name k)))
   (when ks
     (doseq [k ks]
       (.remove goog.net.cookies (name k)))
     )))

