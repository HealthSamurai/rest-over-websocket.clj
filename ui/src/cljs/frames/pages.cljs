(ns frames.pages)

(defonce pages (atom {}))

(defn reg-page
  "register page under keyword for routing"
  [key f & [layout-key]]
  (swap! pages assoc key f))

