(ns ui.routes
  (:require [clojure.string :as str]
            [route-map.core :as route-map]
            [frames.pages :refer [reg-page]]
            [re-frame.core :as rf]))

(def routes
  {:. :index/index})
