(ns retailcal.calendar
  (:require [java-time.api :as jt]
            [tablecloth.api :as tc])
  (:import [java.time.format DateTimeFormatter]))

(defn- parse-date
  "Parses the input date, which can be a string or a java.time.LocalDate instance."
  [date]
  (cond
    (instance? java.time.LocalDate date) date
    (string? date) (jt/local-date date)
    :else (throw (IllegalArgumentException. "Invalid date format"))))

(defn- calc-year-start-date
  "Finds the start date of the retail year given the year, start month, and start day."
  [year start-month start-day]
  (let [first-day-of-month (jt/local-date year (jt/month start-month) 1)
        first-start-day (jt/adjust first-day-of-month :first-in-month start-day)]
    (if (<= (jt/as first-start-day :day-of-month) 4)
      first-start-day
      (jt/minus first-start-day (jt/days 7)))))

(defn ->retail-date
  "
  Converts a given date into retail calendar fields using specified options. Defaults
  to the National Retail Federation's 4-5-4 calendar, described at:
  https://nrf.com/resources/4-5-4-calendar

  Options:

  :calendar-style
  Must be one of \"454\" \"445\" \"544\" (default \"454\").
  Specifies the number of weeks for each month in a given retail quarter.
  \"454\" indicates that each quarter consists of a 4-week month, followed
  by a 5-week month, followed by a 4-week month.

  :start-month
  Must be a month name as a keyword. Default is :february. Indicates the
  calendar month which constitues the first month in the retail year.

  :start-day
  Must be a day of the week as a keyword. Default is :sunday. Indicates
  the day on which each retail week begins.

  Example:

  (->retail-date \"2023-10-01\" :calendar-style \"445\" :start-month :february :start-day :monday)

  (->retail-date \"2025-12-31\")

  Returns a map containing the following fields:
  :calendar-date
  :retail-year
  :retail-year-start-date
  :retail-year-end-date
  :retail-quarter-of-year
  :retail-quarter-start-date
  :retail-quarter-end-date
  :retail-month-of-year
  :retail-month-of-quarter
  :retail-month-start-date
  :retail-month-end-date
  :retail-week-of-year
  :retail-week-of-quarter
  :retail-week-of-month
  :retail-week-start-date
  :retail-week-end-date
  :retail-day-of-year
  :retail-day-of-quarter
  :retail-day-of-month
  :retail-day-of-week
  "
  [date & {:keys [calendar-style start-month start-day]
           :or {calendar-style "454"
                start-month :february
                start-day :sunday}}]
  (let [date
        (parse-date date)

        calendar-year
        (jt/as date :year)

        retail-year-cutoff
        (calc-year-start-date calendar-year start-month start-day)

        retail-year
        (if (jt/> retail-year-cutoff date)
          (dec calendar-year)
          calendar-year)

        retail-year-start-date
        (calc-year-start-date retail-year start-month start-day)

        retail-year-end-date
        (-> (inc retail-year)
            (calc-year-start-date start-month start-day)
            (jt/minus (jt/days 1)))

        retail-day-of-year* ;; asterisk indicates zero-indexed value
        (jt/time-between retail-year-start-date date :days)

        retail-week-of-year*
        (quot retail-day-of-year* 7)

        retail-day-of-week*
        (as-> retail-week-of-year* %
          (* % 7)
          (jt/days %)
          (jt/plus retail-year-start-date %)
          (jt/time-between % date :days))

        quarter-start-weeks*
        [0 13 26 39]

        retail-quarter-of-year*
        (->> quarter-start-weeks*
             (filter #(>= retail-week-of-year* %))
             (count)
             (dec))

        retail-quarter-start-date
        (->> retail-quarter-of-year*
             (* 13 7)
             (jt/days)
             (jt/plus retail-year-start-date))

        retail-quarter-end-date
        (if (= retail-quarter-of-year* 3)
          retail-year-end-date
          (->> (inc retail-quarter-of-year*)
               (* 13 7)
               (dec)
               (jt/days)
               (jt/plus retail-year-start-date)))

        retail-week-of-quarter*
        (->> retail-quarter-of-year*
             (* 13)
             (- retail-week-of-year*))

        month-start-weeks*
        (condp = calendar-style
          "454" [0 4 9]
          "445" [0 4 8]
          "544" [0 5 9]
          (throw (IllegalArgumentException.
                  (str "Invalid calendar style: " calendar-style))))

        retail-month-of-quarter*
        (->> month-start-weeks*
             (filter #(>= retail-week-of-quarter* %))
             (count)
             (dec))

        retail-month-of-year*
        (-> retail-quarter-of-year*
            (* 3)
            (+ retail-month-of-quarter*))

        retail-month-start-date
        (->> retail-month-of-quarter*
             (nth month-start-weeks*)
             (+ (* retail-quarter-of-year* 13))
             (jt/weeks)
             (jt/plus retail-year-start-date))

        retail-month-end-date
        (if (= retail-month-of-year* 11)
          retail-year-end-date
          (if (= retail-month-of-quarter* 2)
            retail-quarter-end-date
            (->> (inc retail-month-of-quarter*)
                 (nth month-start-weeks*)
                 (+ (* retail-quarter-of-year* 13))
                 (* 7)
                 (dec)
                 (jt/days)
                 (jt/plus retail-year-start-date))))

        retail-week-of-month*
        (->> retail-month-of-quarter*
             (nth month-start-weeks*)
             (- retail-week-of-quarter*))]
    {:calendar-date             date
     :retail-year               retail-year
     :retail-quarter-of-year    (inc retail-quarter-of-year*)
     :retail-month-of-year      (inc retail-month-of-year*)
     :retail-month-of-quarter   (inc retail-month-of-quarter*)
     :retail-week-of-year       (inc retail-week-of-year*)
     :retail-week-of-quarter    (inc retail-week-of-quarter*)
     :retail-week-of-month      (inc retail-week-of-month*)
     :retail-day-of-year        (inc retail-day-of-year*)
     :retail-day-of-quarter     (inc (jt/time-between retail-quarter-start-date date :days))
     :retail-day-of-month       (inc (jt/time-between retail-month-start-date date :days))
     :retail-day-of-week        (inc retail-day-of-week*)
     :retail-year-start-date    retail-year-start-date
     :retail-year-end-date      retail-year-end-date
     :retail-quarter-start-date retail-quarter-start-date
     :retail-quarter-end-date   retail-quarter-end-date
     :retail-month-start-date   retail-month-start-date
     :retail-month-end-date     retail-month-end-date
     :retail-week-start-date    (jt/plus retail-year-start-date (jt/weeks retail-week-of-year*))
     :retail-week-end-date      (jt/plus retail-year-start-date (jt/weeks retail-week-of-year*) (jt/days 6))}))

(defn retail-calendar
  "Generates a calendar with given start-date, end-date and specified options (or defaults)."
  [start-date end-date & {:keys [calendar-style start-month start-day]
                          :or {calendar-style "454"
                               start-month :february
                               start-day :sunday}}]
  (let [start (parse-date start-date)
        end (parse-date end-date)
        date-range (take-while #(jt/<= % end)
                               (jt/iterate jt/plus start (jt/days 1)))]
    (map #(->retail-date % :calendar-style calendar-style :start-month start-month :start-day start-day) date-range)))

(defn retail-calendar-dataset
  "Creates a scicloj.tablecloth dataset of a calendar with given start-date, end-date, and specified options (or defaults)."
  [start-date end-date & {:keys [calendar-style start-month start-day]
                          :or {calendar-style "454"
                               start-month :february
                               start-day :sunday}}]
  (-> (retail-calendar start-date end-date :calendar-style calendar-style :start-month start-month :start-day start-day)
      (tc/dataset)
      (tc/select-columns [:calendar-date
                          :retail-year
                          :retail-quarter-of-year
                          :retail-month-of-year
                          :retail-month-of-quarter
                          :retail-week-of-year
                          :retail-week-of-quarter
                          :retail-week-of-month
                          :retail-day-of-year
                          :retail-day-of-quarter
                          :retail-day-of-month
                          :retail-day-of-week
                          :retail-year-start-date
                          :retail-year-end-date
                          :retail-quarter-start-date
                          :retail-quarter-end-date
                          :retail-month-start-date
                          :retail-month-end-date
                          :retail-week-start-date
                          :retail-week-end-date])))

(defn write-retail-calendar!
  "Writes a retail calendar to the specified path with given start-date, end-date, and specified options (or defaults).

  Filename must end in \".csv\" for comma separated values or \".tsv\" for tab separated values. Optional \".gz\" may
  be appeneded to the end to create a compressed file."
  [file-path start-date end-date & {:keys [calendar-style start-month start-day]
                                    :or {calendar-style "454"
                                         start-month :february
                                         start-day :sunday}}]
  (-> (retail-calendar-dataset start-date end-date :calendar-style calendar-style :start-month start-month :start-day start-day)
      (tc/write! file-path)))

(defn retail-year->calendar
  "Generates a calendar for a given retail year using specified options (or defaults)."
  [retail-year & {:keys [calendar-style start-month start-day]
                  :or {calendar-style "454"
                       start-month :february
                       start-day :sunday}}]
  (let [start-date (calc-year-start-date retail-year start-month start-day)
        end-date (-> (inc retail-year)
                     (calc-year-start-date start-month start-day)
                     (jt/minus (jt/days 1)))]
    (retail-calendar-dataset start-date end-date :calendar-style calendar-style :start-month start-month :start-day start-day)))

(defn printable-calendar
  "Creates a printable calendar output for a retail year that mimics the visual format of a standard calendar.
  The year can be specified with the `:retail-year` option, otherwise defaults to the current system time year."
  [& {:keys [retail-year calendar-style start-month start-day]
      :or {retail-year (jt/as (jt/year) :year)
           calendar-style "454"
           start-month :february
           start-day :sunday}}]
  (-> (retail-year->calendar retail-year :calendar-style calendar-style :start-month start-month :start-day start-day)
      (tc/add-columns {:day-of-month (fn [x] (map #(jt/as % :day-of-month) (x :calendar-date)))
                       :weekday (fn [x] (map #(subs (.name (.getDayOfWeek %)) 0 3) (x :calendar-date)))})
      (tc/group-by [:retail-year :retail-quarter-of-year :retail-month-of-year :retail-week-of-year :weekday])
      (tc/aggregate-columns [:day-of-month] #(reduce min %))
      (tc/crosstab [:retail-year :retail-quarter-of-year :retail-month-of-year :retail-week-of-year]
                   [:weekday]
                   {:aggregator #(reduce min (:day-of-month %))})))
