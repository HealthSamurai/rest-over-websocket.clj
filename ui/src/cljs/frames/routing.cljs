(ns frames.routing
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent]
            [re-frame.core :as rf]
            [frames.window-location :refer [parse-querystring gen-query-string]]
            [clojure.string :as str]
            [clojure.set :as set]
            [route-map.core :as route-map]))

(defn dispatch-routes [_]
  (let [fragment (-> (.. js/window -location -hash)
                     (str/replace #"^#" ""))]
    (rf/dispatch [:fragment-changed fragment])))

(rf/reg-sub-raw
 :route-map/breadcrumbs
 (fn [db _] (reaction (:route-map/breadcrumbs @db))))

(rf/reg-sub
 :route-map/current-route
 (fn [db _]
   (assoc (:route-map/current-route db)
          :query (:route-map/query db)
          :layout (:route-map/layout db))))

(defn contexts-diff [old-contexts new-contexts params old-params]
  (let [n-idx (into #{} new-contexts)
        o-idx (into #{} old-contexts)
        to-dispose (set/difference o-idx n-idx)]
    (concat
     (mapv (fn [x] [x :init params]) new-contexts)
     (mapv (fn [x] [x :deinit old-params]) to-dispose))))

(defn mk-breadcrumbs [route context]
  (->> (or (:parents route) [])
       (reduce (fn [acc v]
                 (let [prev (last acc)
                       uri (:uri prev)
                       p  (last (for [[k route] prev :when (= (:. route) (:. v))] k))
                       p (or (get-in route [:params (first p)]) p)
                       id  (:. v)
                       ctx  (get context id)]
                   (conj acc (assoc v
                                    :uri (str uri p  "/")
                                    :ctx ctx))))
               [])
       (filter :breadcrumb)
       (mapv #(select-keys % [:uri :breadcrumb :ctx]))
       (reduce (fn [acc v]
                 (if (keyword? (:breadcrumb v))
                   (conj acc (assoc v :breadcrumb ((:breadcrumb v) (:params route))))
                   (conj acc v))) [])))


(rf/reg-event-fx
 :fragment-changed
 (fn goto [{db :db :as coef} [_ fragment]]
   (let [[location query] (str/split fragment #"[?]")
         route (route-map/match [:. location] (:route-map/routes db))
         path  (-> fragment
                   (str/split #"[?]")
                   first
                   (clojure.string/split #"/")
                   (->> (remove empty?) vec))
         query (if query (parse-querystring query) query)]

     (when-let [related (-> route :parents last :goto)]
       (rf/dispatch [:route-map/redirect (str "#/" (str/join "/" (flatten (concat path related)))) ]))
     (cond
       (nil? route) {:db (assoc db :fragment fragment :route-map/current-route nil)}
       (string? (:match route)) (goto coef [_ (:match route)])
       :else (let [contexts (->> (:parents route) (mapv :context) (filterv identity))
                   path-context (:route-map/path-context db)
                   breadcrumbs (mk-breadcrumbs route path-context)
                   layout (->> (:parents route) (mapv :layout) (filterv identity) last)
                   aliases (->> (:parents route)
                                (mapv :alias)
                                (map-indexed (fn [idx a]
                                               (if a
                                                 {(keyword a) (get path idx)}
                                                 nil)))
                                (remove nil?)
                                (reduce merge {}))
                   path-context (assoc path-context (:match route) query)
                   route (update route :params merge aliases)]
               {:db (assoc db :fragment fragment
                           :route/context contexts
                           :route-map/path path
                           :route-map/layout layout
                           :route-map/breadcrumbs breadcrumbs
                           :route-map/current-route route
                           :route-map/path-context path-context
                           :route-map/query query)

                :dispatch-n (contexts-diff (:route/context db)
                                           contexts
                                           (:params route)
                                           (get-in db [:route-map/current-route :params]))})))))

(rf/reg-event-fx
 :route-map/init
 (fn [cofx [_ routes]]
   {:db (assoc (:db cofx) :route-map/routes routes)
    :history {}}))

(rf/reg-fx
 :history
 (fn [_]
   (aset js/window "onhashchange" dispatch-routes)
   (dispatch-routes nil)))

(rf/reg-fx
 :route-map/redirect
 (fn [href]
   (aset (.-location js/window) "hash" href)))

(rf/reg-event-fx
 :route-map/redirect
 (fn [coef [_ href]]
   {:route-map/redirect href}))

(rf/reg-sub-raw
 :route-map/current-path
 (fn [db [_]]
   (let [cur (reagent/cursor db [:route-map/path])]
     (reaction @cur))))
