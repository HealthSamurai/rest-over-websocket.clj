(ns ^:figwheel-no-load ui.prod
  (:require [ui.core :as core]))

(enable-console-print!)

(core/init! {:base-url "https://test.health-samurai.io"})
