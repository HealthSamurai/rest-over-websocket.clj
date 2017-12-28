(ns app.event-source
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.set]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log])
  (:import [java.sql DriverManager]
           [org.postgresql PGConnection PGProperty]))

(defonce subscriptions (atom {}))

@subscriptions

(defn normalize-msg [x]
  (for [ch (:change x)]
    (cond
      (= "delete" (:kind ch))
      (let [cols (get-in ch [:oldkeys :keynames])
            tbl (get-in ch [:table])
            vals (get-in ch [:oldkeys :keyvalues])]
        {:table (keyword tbl)
         :change :delete
         :row  (apply hash-map (interleave (mapv keyword cols) vals))})

      (= "insert" (:kind ch))
      (let [cols (get-in ch [:columnnames])
            types (get-in ch [:columntypes])
            vals (get-in ch [:columnvalues])
            tbl (get-in ch [:table])
            coersed-vals (mapv (fn [t v]
                                 (cond
                                   (= "jsonb" t) (json/parse-string v keyword)
                                   :else v)) types vals)]
        {:table (keyword tbl)
         :change :insert
         :row  (apply hash-map (interleave (mapv keyword cols) coersed-vals))})
      :else ch)))

(comment
  (normalize-msg {:change [{:kind "delete", :schema "public", :table "messages", :oldkeys {:keynames ["id"], :keytypes ["integer"], :keyvalues [47]}}
                           {:kind "delete", :schema "public", :table "messages", :oldkeys {:keynames ["id"], :keytypes ["integer"], :keyvalues [48]}}
                           {:kind "delete", :schema "public", :table "messages", :oldkeys {:keynames ["id"], :keytypes ["integer"], :keyvalues [49]}} {:kind "delete", :schema "public", :table "messages", :oldkeys {:keynames ["id"], :keytypes ["integer"], :keyvalues [50]}} {:kind "delete", :schema "public", :table "messages", :oldkeys {:keynames ["id"], :keytypes ["integer"], :keyvalues [51]}} {:kind "delete", :schema "public", :table "messages", :oldkeys {:keynames ["id"], :keytypes ["integer"], :keyvalues [52]}} {:kind "delete", :schema "public", :table "messages", :oldkeys {:keynames ["id"], :keytypes ["integer"], :keyvalues [53]}} {:kind "delete", :schema "public", :table "messages", :oldkeys {:keynames ["id"], :keytypes ["integer"], :keyvalues [54]}} {:kind "delete", :schema "public", :table "messages", :oldkeys {:keynames ["id"], :keytypes ["integer"], :keyvalues [55]}}]})

  (normalize-msg
   {:change [{:kind "insert"
              :schema "public"
              :table "rooms"
              :columnnames ["id" "tx" "resource"]
              :columntypes ["integer" "bigint" "jsonb"]
              :columnvalues [6 13 "{\"name\": \"Another room 2\"}"]}]})
  )

(defn dispatch [msg]
  (doseq [m (normalize-msg msg)]
    (println "MESSAGE:" (pr-str msg) " => " m)
    (doseq [[k f] @subscriptions]
      (println "NOTIFY " f)
      (f m))))

(defn add-sub [k f]
  (swap! subscriptions assoc k f))

(defn rm-sub [k]
  (swap! subscriptions dissoc k))

(defn has-sub? [k]
  (get @subscriptions k))

(defn on-message [x]
  ;; (println "raw message:" x)
  (dispatch (json/parse-string x keyword)))

(defn close-connection [{conn :conn}]
  (when conn (.close conn)))

(defn connection [{uri :uri usr :user pwd :password slot-name :slot decod :decoder}]
  (def pr (java.util.Properties.))

  (.set PGProperty/USER pr usr)
  (.set PGProperty/REPLICATION pr "database")
  (.set PGProperty/PASSWORD pr pwd)
  (.set PGProperty/PREFER_QUERY_MODE pr "simple")
  (.set PGProperty/ASSUME_MIN_SERVER_VERSION pr "9.5")

  (let [slot-name (or slot-name "test_slot")
        conn (-> (DriverManager/getConnection uri pr)
                 (.unwrap PGConnection))
        _  (try
             (-> conn
                 (.prepareStatement (str "DROP_REPLICATION_SLOT " slot-name))
                 (.execute))
             (catch Exception e (println e)))

        slot (.. conn
                 (getReplicationAPI)
                 (createReplicationSlot)
                 (logical)
                 (withSlotName slot-name)
                 (withOutputPlugin (or decod "wal2json"))
                 (make))

        stream (.. conn
                   (getReplicationAPI)
                   (replicationStream)
                   (logical)
                   (withSlotName slot-name)
                   (withSlotOption "include-xids", false)
                   (start))]

    (future
      (loop []
        (let [msg (.read stream)
              src (.array msg)
              off (.arrayOffset msg)
              lsn (.getLastReceiveLSN stream)]
          (#'on-message (String. src  off  (- (count src) off)))
          (println "LSN:" (str lsn))
          (.setAppliedLSN stream lsn)
          (.setFlushedLSN stream lsn))
        (recur)))

    {:conn conn
     :slot slot-name}))

(comment 
  (defonce conn
    (connection
     {:uri "jdbc:postgresql://localhost:5444/postgres"
      :user "postgres"
      :password "secret"
      :slot "test_slot2"
      :decoder "wal2json"}))

  conn

  (close-connection conn)

  (add-sub :test (fn [x] (println "SUB: " x)))

  )



