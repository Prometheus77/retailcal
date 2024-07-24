(ns retailcal.calendar
  (:require [java-time.api :as jt]
            [tablecloth.api :as tc]))

(defn- parse-date
  "Parses the input date, which can be a string or a java.time.LocalDate instance."
  [date]
  (cond
    (instance? java.time.LocalDate date) date
    (string? date) (jt/local-date date)
    :else (throw (IllegalArgumentException. "Invalid date format"))))

(defn- day-value
  "Returns the numeric value of the given day keyword."
  [day]
  (case day
    :sunday 7
    :monday 1
    :tuesday 2
    :wednesday 3
    :thursday 4
    :friday 5
    :saturday 6))

(defn- retail-day-of-week
  "Calculates the retail day of the week given a date and the start day of the week."
  [date start-day]
  (let [weekday (jt/as date :day-of-week)
        start-day-value (day-value start-day)]
    (-> (- weekday start-day-value) (+ 7) (mod 7) inc)))

(defn- retail-year-start-date
  "Finds the start date of the retail year given the year, start month, and start day."
  [year start-month start-day]
  (let [first-day-of-month (jt/local-date year (jt/month start-month) 1)
        first-start-day (jt/adjust first-day-of-month :first-in-month start-day)]
    (if (<= (jt/as first-start-day :day-of-month) 4)
      first-start-day
      (jt/minus first-start-day (jt/days 7)))))

(defn- find-retail-year
  "Determines the retail year for a given date, start month, and start day."
  [date start-month start-day]
  (let [year (jt/as date :year)
        candidates (map #(retail-year-start-date % start-month start-day)
                        [(dec year) year (inc year) (+ 2 year)])
        candidate (last (filter #(jt/>= date %) candidates))]
    (jt/as candidate :year)))

(defn- retail-month-and-week
  "Calculates the retail month and week within the month given the calendar style and week of the year."
  [calendar-style week-of-year]
  (let [quarter-pattern (condp = calendar-style
                          "454" [4 5 4]
                          "445" [4 4 5]
                          "544" [5 4 4]
                          (throw (IllegalArgumentException. (str "Invalid calendar style: " calendar-style))))
        first-weeks (->> quarter-pattern (repeat 4) flatten (reductions + 1) butlast)
        month-start-week (last (take-while #(<= % week-of-year) first-weeks))
        month-of-year-index (.indexOf first-weeks month-start-week)
        month-of-year (inc month-of-year-index)
        week-of-month (inc (- week-of-year (nth first-weeks month-of-year-index)))
        quarter-of-year (-> month-of-year dec (quot 3) inc)
        quarter-start-week (nth first-weeks (* (dec quarter-of-year) 3))
        week-of-quarter (inc (- week-of-year quarter-start-week))]
    {:month-of-year month-of-year
     :quarter-of-year quarter-of-year
     :month-of-quarter (-> month-of-year dec (mod 3) inc)
     :week-of-quarter week-of-quarter
     :week-of-month week-of-month}))

(defn ->retail-date
  "Converts a given date into retail calendar fields."
  ([date & {:keys [calendar-style start-month start-day]
            :or {calendar-style "454"
                 start-month :february
                 start-day :sunday}}]
   (let [date (parse-date date)
         year (find-retail-year date start-month start-day)
         year-start-date
         (retail-year-start-date year start-month start-day)
         week-of-year (inc (quot (jt/time-between year-start-date date :days) 7))
         {:keys [month-of-year week-of-month quarter-of-year month-of-quarter week-of-quarter]}
         (retail-month-and-week calendar-style week-of-year)
         day-of-week (retail-day-of-week date start-day)
         day-of-month (-> week-of-month
                          dec
                          (* 7)
                          (+ day-of-week))]
     {:calendar-date (str date)
      :retail-year year
      :retail-quarter-of-year quarter-of-year
      :retail-month-of-year month-of-year
      :retail-week-of-year week-of-year
      :retail-day-of-year (inc (jt/time-between year-start-date date :days))
      :retail-month-of-quarter month-of-quarter
      :retail-week-of-quarter week-of-quarter
      :retail-week-of-month week-of-month
      :retail-day-of-quarter (-> week-of-quarter dec (* 7) (+ day-of-week))
      :retail-day-of-month day-of-month
      :retail-day-of-week day-of-week})))

(defn ->retail-dates
  "Generates retail dates for a vector of dates or a range defined by start-date and end-date."
  ([dates & {:keys [calendar-style start-month start-day]
             :or {calendar-style "454"
                  start-month :february
                  start-day :sunday}}]
   (let [date-seq (map parse-date dates)]
     (map #(->retail-date % :calendar-style calendar-style :start-month start-month :start-day start-day) date-seq))))

(defn ->retail-date-range
  "Generates retail dates for a range defined by start-date and end-date."
  [start-date end-date & {:keys [calendar-style start-month start-day]
                          :or {calendar-style "454"
                               start-month :february
                               start-day :sunday}}]
  (let [start-date (parse-date start-date)
        end-date (parse-date end-date)
        date-seq (take-while #(jt/<= % end-date)
                             (jt/iterate jt/plus start-date (jt/days 1)))]
    (map #(->retail-date % :calendar-style calendar-style :start-month start-month :start-day start-day) date-seq)))

(defn ->retail-dates-ds
  "Returns a tablecloth dataset of retail dates from a vector of calendar dates."
  [dates & {:keys [calendar-style start-month start-day]
            :or {calendar-style "454"
                 start-month :february
                 start-day :sunday}}]
  (->
   (->retail-dates dates :calendar-style calendar-style :start-month start-month :start-day start-day)
   (tc/dataset)))

(defn ->retail-date-range-ds
"Returns a tablecloth dataset of retail dates from a vector of calendar dates."
  [start-date end-date file-path & {:keys [calendar-style start-month start-day]
                                    :or {calendar-style "454"
                                         start-month :february
                                         start-day :sunday}}]
  (->
   (->retail-date-range start-date end-date :calendar-style calendar-style :start-month start-month :start-day start-day)
   (tc/dataset)))

(defn write-dates!
  "Writes a vector of retail date maps to disk in the specified format (include either .csv or .tsv in the filename)."
  [dates file-path & {:keys [calendar-style start-month start-day]
                      :or {calendar-style "454"
                           start-month :february
                           start-day :sunday}}]
  (->
   (->retail-dates-ds dates :calendar-style calendar-style :start-month start-month :start-day start-day)
   (tc/write! file-path)))


(defn write-date-range!
  "Writes a range of retail dates to disk in the specified format (include either .csv or .tsv in the filename)."
  [start-date end-date file-path & {:keys [calendar-style start-month start-day]
                                    :or {calendar-style "454"
                                         start-month :february
                                         start-day :sunday}}]
  (->
   (->retail-date-range-ds start-date end-date :calendar-style calendar-style :start-month start-month :start-day start-day)
   (tc/write! file-path)))
