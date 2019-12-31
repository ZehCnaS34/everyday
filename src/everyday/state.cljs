(ns everyday.state
  (:require [om.next :as om]
            [everyday.date :refer [is-leap-year current-year]]
            [re-frame.core :as rf]))


(defn date [year month day] (str year "-" month "-" day))

(defn day [i]
  {:day i})

(defn build-days [amount]
  (into []
        (map-indexed
          (fn [element index] (day index))
          (range amount))))

(def app-state
  {:count    0
   :now      (.now js/Date)
   :edit     {}
   :tasks    {}
   :task-order []
   :calendar [[0 31]
              [1 (if (is-leap-year (current-year)) 28 29)]
              [2 31]
              [3 30]
              [4 31]
              [5 30]
              [6 31]
              [7 31]
              [8 30]
              [9 31]
              [10 30]
              [11 31]]})

;; storage garbage
(def ls-key "everyday")

(defn state->local-store
  [state]
  (.setItem js/localStorage ls-key (str state)))

(rf/reg-cofx
  :local-store-state
  (fn [cofx _]
    (assoc cofx :local-store-state
                (some-> (.getItem js/localStorage ls-key)
                        (cljs.reader/read-string)))))

(def ->local-store (rf/after state->local-store))

(def state-interceptors [->local-store])

;; database shit

(rf/reg-event-fx
  :initialize
  [(rf/inject-cofx :local-store-state)]
  (fn [{:keys [local-store-state]} _]
    {:db (merge app-state local-store-state)}))


;; events

(rf/reg-event-db
  :edit
  (fn [db [_ year month day]]
    (assoc-in db [:edit] {:year  year
                          :month month
                          :day   day})))

(rf/reg-event-db
  :add-task
  state-interceptors
  (fn [db _]
    (let [{:keys [year month day]} (:edit db)
          task-id (str (random-uuid))]
      (-> db
          (assoc-in [:tasks task-id] {:date (date year month day) :value "" :type "personal" :complete false})
          (update-in [:task-order] conj task-id)))))

(rf/reg-event-db
  :remove-task
  state-interceptors
  (fn [db [_ id]]
    (-> db
        (update-in [:tasks] dissoc id)
        (update-in [:task-order] (fn [lst] (filter #(not= id %) lst))))))


(rf/reg-event-db
  :update-task
  state-interceptors
  (fn [db [_ id value]]
    (assoc-in db [:tasks id :value] value)))

(rf/reg-event-db
  :update-task-type
  state-interceptors
  (fn [db [_ id type]]
    (assoc-in db [:tasks id :type] type)))

(rf/reg-event-db
  :toggle-task-completed
  state-interceptors
  (fn [db [_ id]]
    (update-in db [:tasks id :complete] not)))


;; views

(rf/reg-sub
  :year
  (fn [db _]
    (:calendar db)))

(defn tasks-for-date [year month day task-map]
  (into {} (filter (fn [[_ task]] (= (date year month day) (:date task))) task-map)))

(defn task-map->vec [task-order task-map]
  (reduce (fn [acc task-id]
            (if-let [task (task-map task-id)]
              (conj acc (merge task {:id task-id}))
              acc))
          []
          task-order))

(rf/reg-sub
  :focused-tasks
  (fn [db _]
    (let [{:keys [year month day]} (:edit db)]
      (->> (:tasks db)
           (tasks-for-date year month day)
           (task-map->vec (:task-order db))))))

(rf/reg-sub
  :can-edit?
  (fn [db _]
    (not (nil? (keys (:edit db))))))

(rf/reg-sub
  :edit
  (fn [db _]
    (:edit db)))

(rf/reg-sub
  :can-view?
  (fn [db _]
    (not (nil? (keys (:edit db))))))

(rf/reg-sub
  :selected?
  (fn [db [_ year month day]]
    (= (:edit db) {:year year :month month :day day})))

(rf/reg-sub
  :tasks-for
  (fn [db [_ year month day]]
    (->> (:tasks db)
         (tasks-for-date year month day)
         (task-map->vec (:task-order db)))))

(rf/reg-sub
  :all
  (fn [db _]
    db))
