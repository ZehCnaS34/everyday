(ns everyday.core
  (:require [everyday.state :refer [app-state]]
            [everyday.calendar :refer [year]]
            [clojure.pprint :refer [pprint]]
            [re-frame.core :as rf]
            [reagent.core :as r]))


(defn fragments [& children]
  (apply array (for [[index child] (map-indexed vector children)]
                 ^{:key (str "index-" index)} (r/as-element child))))

(defn event-value [e] (.. e -target -value))

(defn handle-enter [e]
  (when (= (.. e -key) "Enter")
    (.preventDefault e)
    (rf/dispatch [:add-task])))

(defn time-view []
  (let [{:keys [year month day]} @(rf/subscribe [:edit])]
    [:p (str year "/" (inc month) "/" (inc day))]))

(defn edit []
  (let [
        tasks @(rf/subscribe [:focused-tasks])
        can-view? @(rf/subscribe [:can-view?])
        can-edit? @(rf/subscribe [:can-edit?])]
    (when can-edit?
      [:div.edit-panel
       [time-view]
       [:button {:on-click #(rf/dispatch [:add-task])} "+"]
       [:ol
        (for [{:keys [id value date complete]} tasks]
          ^{:key id} [:li
                      [:input {:type :checkbox :checked complete :on-change #(rf/dispatch-sync [:toggle-task-completed id])}]
                      [:input {:on-change   #(rf/dispatch-sync [:update-task id (event-value %)])
                               :tab-index   "0"
                               :on-key-down handle-enter
                               :value       value}]
                      [:select {:on-change #(rf/dispatch-sync [:update-task-type id (.. % -target -value)])}
                       [:option {:value "personal"} "personal"]
                       [:option {:value "errand"} "errand"]
                       [:option {:value "work"} "work"]]
                      [:button {:tab-index "1" :on-click #(rf/dispatch-sync [:remove-task id])} "-"]])]])))

(defn print-state-button []
  (let [db @(rf/subscribe [:all])]
    [:button {:on-click #(pprint db)} "Print"]))


(defn app []
  (let [y @(rf/subscribe [:year])]
    (fragments
      [year y]
      [edit]
      [print-state-button])))

(r/render [app] (js/document.getElementById "app"))

(defn on-js-reload [])

(rf/dispatch-sync [:initialize])
