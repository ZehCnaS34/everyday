(ns everyday.calendar
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [goog.dom :as gdom]
            [re-frame.core :as rf]
            [everyday.date :as d]
            [clojure.pprint :refer [pprint]]
            [clojure.string :as s]))

(defn log [& a]
  (apply js/console.log a))

(defn make-id [] (str (random-uuid)))


(defn date-view [dn]
  [:p (get ["jan" "feb" "mar" "apr" "may" "jun" "jul" "aug" "sep" "oct" "nov" "dec"]
           dn)])

(defn only-true [m] (filter (fn [[key value]] value) m))
(defn classes [m]
  (s/join " " (->> m
                   only-true
                   keys
                   (map name)
                   (into []))))

(defn past? [{:keys [day month]}]
  (or (> (d/month) month)
      (and (> (d/day) day)
           (>= (d/month) month))))

(defn today? [{:keys [day month]}]
  (and (= (d/month) month)
       (= (d/day) day)))

(defn day [year month day]
  (let [tasks @(rf/subscribe [:tasks-for year month day])
        selected? @(rf/subscribe [:selected? year month day])]
    [:div.day {:on-click #(rf/dispatch [:edit year month day])
               :class    (classes {:past     (past? {:month month :day day})
                                   :today    (today? {:month month :day day})
                                   :selected selected?})}
     (for [{:keys [id type complete]} tasks]
       ^{:key id} [:div.task {:class (classes {:personal (= type "personal")
                                               :work     (= type "work")
                                               :errand   (= type "errand")
                                               :complete complete})}])]))


(defn month [{y :year m :month ds :days}]
  [:div.month
   [date-view m]
   (for [d (range ds)]
     ^{:key (str m "-" d)} [day y m d])])

(defn day-index []
  [:div.day-index
   [:p "*"]
   (for [d (range 1 32)]
     ^{:key d} [:div.day.index d])])

(defn year [months]
  (let [y (d/current-year)]
    [:div.year-container
     [:p y]
     [:div.year
      [day-index]
      (for [[m ds] months]
        ^{:key (str y "-" m)} [month {:year y :month m :days ds}])]]))