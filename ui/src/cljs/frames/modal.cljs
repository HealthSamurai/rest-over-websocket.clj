(ns frames.modal
  (:require
   [re-frame.core :as rf]))

(rf/reg-event-db
 ::modal
 (fn [db [_ props]]
   (assoc db :modal props)))

(rf/reg-event-db
 :modal/modal
 (fn [db [_ props]]
   (assoc db :modal props)))

(rf/reg-fx
 :modal/modal
 (fn [m]
   (rf/dispatch [::modal m])))

(rf/reg-event-db
 ::modal-close
 (fn [db _]
   (dissoc db :modal)))

(defn close [] (rf/dispatch [::modal-close]) )

(rf/reg-fx ::modal-close close )
(rf/reg-fx :modal/close close)

(rf/reg-sub
 ::modal
 (fn [db _]
   (get db :modal)))

(defn modal []
  (let [props (rf/subscribe [::modal])]
    (fn []
      (when @props
        [:div.modal-frame
         [:div.modal-overlay {:style {:z-index "1002" :display "block" :opacity "0.5"}}]
         [:div#modal.modal.open {:style {:z-index "1003" :display "block" :opacity "1"
                                         :transform "scaleX(1)" :top "10%"}}
          [:div.modal-content
           [:h4 (or (:header @props) "Header")]
           [:span (:content @props) ]]
          [:div.modal-footer
           [:a.btn {:on-click #(rf/dispatch (:action @props))} (or (:action-label @props) "Принять")]
           [:a.btn-flat {:on-click #(rf/dispatch [::modal-close])} "Отмена"]
           ]]]))))
