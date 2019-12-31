(ns everyday.date)

(defn date [] (js/Date.))

(defn year
  ([date] (.getYear date))
  ([] (year (date))))

(defn month
  ([date] (.getMonth date))
  ([] (month (date))))

(defn day
  ([date] (.getDate date))
  ([] (day (date))))

(defn current-year
  []
  (+ 1900 (year)))

(defn- divisible [n t]
  (= 0 (mod n t)))

(defn is-leap-year
  [y]
  (or (and (divisible y 4)
           (not (divisible y 100)))
      (divisible y 400)))


