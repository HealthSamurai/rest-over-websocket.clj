(ns ui.routes
  (:require [clojure.string :as str]
            [route-map.core :as route-map]
            [frames.pages :refer [reg-page]]
            [re-frame.core :as rf]))

(def routes
  {:. :index/index
   "rooms" {[:id] {:. :rooms/show}}})

(defn to-query-params [params]
  (->> params
       (map (fn [[k v]] (str (name k) "=" v)))
       (str/join "&")))

(defn href [& parts]
  (let [params (if (map? (last parts)) (last parts) nil)
        parts (if params (butlast parts) parts)
        url (str "/" (str/join "/" (map (fn [x] (if (keyword? x) (name x) (str x))) parts)))]
    (when-not  (route-map/match [:. url] routes)
      (.error js/console (str url " is not matches routes")))
    (str "#" url (when params (str "?" (to-query-params params))))))
