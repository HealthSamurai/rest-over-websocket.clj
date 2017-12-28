(ns ui.page.index
  (:require [re-frame.core :as rf]
            [frames.pages :refer [reg-page]]))

(defn index [params]
  [:h2 "hello world"])

(reg-page :index/index index)
