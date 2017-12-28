(ns frames.window-location
  (:refer-clojure :exclude [get set!])
  (:require [re-frame.core :as rf]
            [clojure.string :as str]))

(defn parse-querystring [s]
  (-> (str/replace s #"^\?" "")
      (str/split #"&")
      (->>
       (reduce (fn [acc kv]
                 (let [[k v] (str/split kv #"=" 2)]
                   (let [k (keyword k)
                         v (js/decodeURIComponent v)
                         q (k acc)]
                     (if q
                       (let [q (if (vector? q) q [q])]
                         (assoc acc k (conj q v)))
                       (assoc acc k v)))))
               {}))))



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

(defn gen-query-string [params]
  (->> params
       to-query
       (str "?")))

(defn get-location []
  (let [loc    (.. js/window -location)
        href   (.. loc -href)
        search (.. loc -search)]
    {:href href
     :query-string (parse-querystring search)
     :url (first (str/split href #"#"))
     :hash (str/replace (or (.. loc -hash) "") #"^#" "")
     :host  (.. loc -host)
     :origin (.. loc -origin)
     :protocol (.. loc -protocol)
     :hostname (.. loc -hostname)
     :search search}))

(defn window-location [coef & opts]
  (assoc coef :location (get-location)))

(rf/reg-cofx :window-location window-location)
