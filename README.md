# retailcal

retailcal is a library for converting between standard (Gregorian) calendars and
various retail calendars such as the ones used by the National Retail
Foundation. It supports the 4-5-4, 4-4-5, and 5-4-4 formats, named for the
number of retail weeks per retail month, repeated in sequence four times during
the year. It also automatically detects when a retail year needs to be 53
retail weeks.

## Installation

Download from http://example.com/FIXME.

## Usage

The primary use case is to determine the retail year, retail week, and retail
month of a given date in the Gregorian calendar. The primary function to do this
is `(->retail-date <date>)`. This takes either a java.time.LocalDate or a
string that can be coerced to java.time.LocalDate, and returns a map containing
the following fields:

* `:calendar-date`
* `:retail-year`
* `:retail-quarter-of-year`
* `:retail-month-of-year`
* `:retail-month-of-quarter`
* `:retail-week-of-year`
* `:retail-week-of-quarter`
* `:retail-week-of-month`
* `:retail-day-of-year`
* `:retail-day-of-quarter`
* `:retail-day-of-month`
* `:retail-day-of-week`
* `:retail-year-start-date`
* `:retail-year-end-date`
* `:retail-quarter-start-date`
* `:retail-quarter-end-date`
* `:retail-month-start-date`
* `:retail-month-end-date`
* `:retail-week-start-date`
* `:retail-week-end-date`

The algorithm used to determine the structure of a retail year is as follows:

* Find `:retail-year-start-date`, or the first day of the retail year, by
looking for the calendar date of the first `:start-day` in the `:start-month`
for the calendar year of `<date>`. If the day of the month for that day is <= 4
(e.g. February 1-4), that date will be the first day of the `:retail-year`. If
the calendar date is >4 (e.g. February 5-7), `:retail-year-start-date` will
be one week before that date.

* Based on `:retail-year-start-date`, follow the `:calendar-style` by assigning
the correct number of weeks to each month. See details on the `:calendar-style`
below.

* Using the above year, quarter, month, and week mappings, determine the start
and end dates of each period.

## Options

In addition to the mandatory date, several options are available to specify the
structure of the retail calendar you are using.

`:calendar-style` must be one of `"454"`, `"544"`, or `"445"` (default is `"454"`),
and represents the structure of retail weeks per retail month in a given retail
quarter. As an example, `"454"` style means the first retail month of each retail
quarter contains 4 retail weeks, the second contains 5, and the third contains
4.

`:start-month` specifies the first month of the retail year (default is
`:february`). The `:retail-year` will match the calendar year of
`:start-month`, so the a retail calendar that starts on February 4, 2024 will
be `:retail-year` 2024 (including January 1-February 1, 2025).

`:start-day` specifies the first day of of the retail week
(default is `:sunday`).

## Examples

`(->retail-date "2024-05-05")`

returns the following map:

```
{:retail-day-of-quarter 1,
 :retail-week-end-date #object[java.time.LocalDate 0x3152c06 "2024-05-11"],
 :retail-week-of-month 1,
 :retail-week-of-quarter 1,
 :calendar-date #object[java.time.LocalDate 0x3f2cf455 "2024-05-05"],
 :retail-day-of-year 92,
 :retail-quarter-start-date
 #object[java.time.LocalDate 0x4a3aa368 "2024-05-05"],
 :retail-month-start-date #object[java.time.LocalDate 0x47f009c7 "2024-05-05"],
 :retail-year-start-date #object[java.time.LocalDate 0x3bc47e66 "2024-02-04"],
 :retail-year 2024,
 :retail-day-of-month 1,
 :retail-week-start-date #object[java.time.LocalDate 0xeb97117 "2024-05-05"],
 :retail-year-end-date #object[java.time.LocalDate 0x6ca5842 "2025-02-01"],
 :retail-quarter-end-date #object[java.time.LocalDate 0x45c74fcb "2024-08-03"],
 :retail-month-of-year 4,
 :retail-quarter-of-year 2,
 :retail-week-of-year 14,
 :retail-month-end-date #object[java.time.LocalDate 0x2cf6a5e2 "2024-06-01"],
 :retail-month-of-quarter 1,
 :retail-day-of-week 1}
```

`(retail-calendar "2020-01-01" "2020-01-31" :calendar-style "544" :start-month :january)`

will return a sequence of maps, one for each day between January 1 and 31, 2020.

`(retail-calendar-dataset "2030-01-01" "2030-12-31" :start-day :saturday)`

will return a `scicloj.tablecloth` dataset with one column per field and one row per calendar date.

`(write-retail-calendar! "monday-445.csv" "1970-01-01" "2099-12-31" :calendar-style "445" :start-day :monday)`

will write a .csv file to disk containing one row for each date from January 1, 1970 to December 31, 2099.

`(retail-year->calendar 2020)`

will return a `scicloj.tablecloth` dataset with one row for every day in retail year 2020

`(printable-calendar :retail-year 2021 :calendar-style "544")` (NOTE: experimental)

will create a human-readable calendar that can be used to check dates, convert to a print-ready calendar, etc., per the below:

```
_unnamed [52 8]:

|      rows/cols | SUN | MON | TUE | WED | THU | FRI | SAT |
|----------------|----:|----:|----:|----:|----:|----:|----:|
|   [2021 1 1 1] |  31 |   1 |   2 |   3 |   4 |   5 |   6 |
|   [2021 1 1 2] |   7 |   8 |   9 |  10 |  11 |  12 |  13 |
|   [2021 1 1 3] |  14 |  15 |  16 |  17 |  18 |  19 |  20 |
|   [2021 1 1 4] |  21 |  22 |  23 |  24 |  25 |  26 |  27 |
|   [2021 1 1 5] |  28 |   1 |   2 |   3 |   4 |   5 |   6 |
|   [2021 1 2 6] |   7 |   8 |   9 |  10 |  11 |  12 |  13 |
|   [2021 1 2 7] |  14 |  15 |  16 |  17 |  18 |  19 |  20 |
|   [2021 1 2 8] |  21 |  22 |  23 |  24 |  25 |  26 |  27 |
|   [2021 1 2 9] |  28 |  29 |  30 |  31 |   1 |   2 |   3 |
|  [2021 1 3 10] |   4 |   5 |   6 |   7 |   8 |   9 |  10 |
|            ... | ... | ... | ... | ... | ... | ... | ... |
| [2021 4 10 42] |  14 |  15 |  16 |  17 |  18 |  19 |  20 |
| [2021 4 10 43] |  21 |  22 |  23 |  24 |  25 |  26 |  27 |
| [2021 4 10 44] |  28 |  29 |  30 |   1 |   2 |   3 |   4 |
| [2021 4 11 45] |   5 |   6 |   7 |   8 |   9 |  10 |  11 |
| [2021 4 11 46] |  12 |  13 |  14 |  15 |  16 |  17 |  18 |
| [2021 4 11 47] |  19 |  20 |  21 |  22 |  23 |  24 |  25 |
| [2021 4 11 48] |  26 |  27 |  28 |  29 |  30 |  31 |   1 |
| [2021 4 12 49] |   2 |   3 |   4 |   5 |   6 |   7 |   8 |
| [2021 4 12 50] |   9 |  10 |  11 |  12 |  13 |  14 |  15 |
| [2021 4 12 51] |  16 |  17 |  18 |  19 |  20 |  21 |  22 |
| [2021 4 12 52] |  23 |  24 |  25 |  26 |  27 |  28 |  29 |
```

### Bugs

Please submit bug reports or feature requests to https://github.com/Prometheus77/retailcal/

## License

Copyright © 2024

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
