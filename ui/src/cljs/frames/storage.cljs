(ns frames.storage
  (:require [re-frame.core :as rf] ))

(defn keywordize [x]
  (js->clj x :keywordize-keys true))

(defn remove-item
  [key]
  (.removeItem (.-localStorage js/window) key))

(defn set-item
  [key val]
  (let [val (->> val clj->js (.stringify js/JSON))]
    (.setItem (.-localStorage js/window) (name key) val)))

(defn get-item
  [key]
  (try (->> key
            name
            (.getItem (.-localStorage js/window))
            (.parse js/JSON)
            (keywordize))
       (catch js/Object e (do (remove-item key) nil))))

(rf/reg-cofx
 ::get
 (fn [coeffects key]
   (assoc-in coeffects [:storage key] (get-item key))))

(rf/reg-fx
 :storage/get
 (fn [coeffects key]
   (assoc-in coeffects [:storage key] (get-item key))))

(rf/reg-fx
 ::set
 (fn [{k :key v :value :as opts}]
   (set-item k v)))

(rf/reg-fx
 :storage/set
 (fn [{k :key v :value :as opts}]
   (set-item k v)))

(rf/reg-fx
 ::remove
 (fn [{k :key}] (remove-item k)))
