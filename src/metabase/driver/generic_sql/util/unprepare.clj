(ns metabase.driver.generic-sql.util.unprepare
    "Utility functions for converting a prepared statement with `?` into a plain SQL query."
    (:require [clojure.string :as str]
              [honeysql.core :as hsql]
              [metabase.util :as u]
              [metabase.util.honeysql-extensions :as hx])
    (:import java.util.Date))

(defprotocol ^:private IUnprepare
  (^:private unprepare-arg ^String [this settings]))

(extend-protocol IUnprepare
                 nil     (unprepare-arg [this _] "NULL")
                 String  (unprepare-arg [this {:keys [quote-escape]}] (str \' (str/replace this "'" (str quote-escape "'")) \')) ; escape single-quotes
                 Boolean (unprepare-arg [this _] (if this "TRUE" "FALSE"))
                 Number  (unprepare-arg [this _] (str this))
                 Date    (unprepare-arg [this {:keys [iso-8601-fn]}] (first (hsql/format (hsql/call (keyword "")  (hx/literal (u/date->iso-8601 this)))))))

(defn unprepare
  "Convert a normal SQL `[statement & prepared-statement-args]` vector into a flat, non-prepared statement."
  ^String [[sql & args] & {:keys [quote-escape iso-8601-fn], :or {quote-escape "\\\\", iso-8601-fn :timestamp}}]
  (loop [sql sql, [arg & more-args, :as args] args]
    (if-not (seq args)
            sql
            (recur (str/replace-first sql #"(?<!\?)\?(?!\?)" (unprepare-arg arg {:quote-escape quote-escape, :iso-8601-fn iso-8601-fn}))
              more-args))))
