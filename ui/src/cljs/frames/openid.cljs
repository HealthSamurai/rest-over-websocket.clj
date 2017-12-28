(ns frames.openid
  (:require
   [clojure.spec.alpha :as s]
   [re-frame.core :as rf]
   [frames.cookies :as cookies]
   [frames.storage :as storage]
   [clojure.string :as str]
   [frames.redirect :as redirecy]
   [goog.crypt.base64 :refer [encodeString decodeString]]))

(defn ^:export decode
  [token]
  (let [segments (s/conform (s/cat :header string? :payload string? :signature string?)
                            (str/split token "."))]
    (if-not (map? segments)
      (throw (js/Error. "invalid token"))
      (let [header (.parse js/JSON (js/atob (:header segments)))
            payload (.parse js/JSON (js/atob (:payload segments)))]
        payload))))

(defn check-token []
  (let [hash (when-let [h (.. js/window -location -hash)] (str/replace h  #"^#" ""))]
    (when (str/index-of hash "id_token")
      (let [token (->> (str/split hash "&")
                       (filter #(str/starts-with? % "id_token="))
                       (map (fn [x] (str/replace x #"^id_token=" "")))
                       (first))
            jwt (js->clj (decode token) :keywordize-keys true)]
        (assoc jwt :id_token token)))))

(rf/reg-cofx
 ::jwt
 (fn [coeffects]
   (assoc-in coeffects [:jwt] (check-token))))

(rf/reg-event-fx
 :auth/logout
 (fn [coef [_]]
   {::cookies/remove {:keys ["auth" "account"]}
    ::storage/remove {:key "account"}
    :dispatch [:core/initialize]}))

(rf/reg-sub
 :auth/groups
 (fn [db _] (get-in db [:auth :groups])))
