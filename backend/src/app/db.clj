(ns app.db
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.set]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log])
  (:import [java.sql DriverManager]
           [org.postgresql PGConnection PGProperty]))


(defonce state (atom {}))

(defn close-replication-connection []
  (when-let  [conn (get @state :repl-conn)]
    (.close conn)
    (swap! state dissoc :repl-conn)))

(defn replication-connection
  [{uri :uri usr :user pwd :password slot-name :slot decod :decoder} cb]
  (close-replication-connection)

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

    (swap! state assoc :repl-conn conn :slot slot-name)
    (future
      (loop []
        (let [msg (.read stream)
              src (.array msg)
              off (.arrayOffset msg)
              lsn (.getLastReceiveLSN stream)]
          (cb (String. src  off  (- (count src) off)))
          (println "LSN:" (str lsn))
          (.setAppliedLSN stream lsn)
          (.setFlushedLSN stream lsn))
        (recur)))))

(defn dispatch [msg]
  (println "MESSAGE:" msg))

(defn on-message [x]
  (dispatch (json/parse-string x keyword)))

(comment 
  (defonce repl-conn
    (replication-connection
     {:uri "jdbc:postgresql://localhost:5444/postgres"
      :user "postgres"
      :password "secret"
      :slot "test_slot"
      :decoder "wal2json"}
     #'on-message))

  repl-conn)



