(ns app.pg
  (:require [honeysql.core :as honey]
            [cheshire.core :as json]
            [honeysql.format :as sqlf]
            [clojure.java.jdbc :as jdbc])
  (:import [org.postgresql.util PGobject]))

;; use java

(def db {:dbtype "postgresql"
         :connection-uri "jdbc:postgresql://localhost:5444/postgres?stringtype=unspecified&user=postgres&password=secret"})

(def raw honey/raw)

(sqlf/register-clause! :returning 230)

(defmethod sqlf/format-clause
  :returning
  [[_ fields] sql-map]
  (str "RETURNING "
       (when (:modifiers sql-map)
         (str (sqlf/space-join (map (comp clojure.string/upper-case name)
                                    (:modifiers sql-map)))
              " "))
       (sqlf/comma-join (map sqlf/to-sql fields))))


(defmulti coerse (fn [x] (type x)))

(defmethod coerse :default [x] x)

(defmethod coerse
  PGobject
  [pgobj]
  (let [type  (.getType pgobj)
        value (.getValue pgobj)]
    (case type
      "jsonb"  (json/parse-string value true)
      value)))

(defmethod coerse
  clojure.lang.LazySeq
  [col]
  (mapv coerse col))

(defmethod coerse
  clojure.lang.PersistentArrayMap
  [h]
  (reduce (fn [acc [k v]] (assoc acc k (coerse v))) {} h))

(defn exec [db sql]
  (jdbc/execute! db sql))


(defn q [db hsql]
  (->> (jdbc/query db (if (string? hsql) hsql (honey/format hsql)))
       (mapv coerse)))

(defn jsonb-row [x]
  (merge (:resource x) (dissoc x :resource)))

(defn jsonb-query [db hsql]
  (->> (q db hsql)
       (mapv jsonb-row)))

(defn jsonb-insert [db tbl row]
  (first (jsonb-query db {:insert-into tbl
                          :values [(assoc (update row :resource json/generate-string)
                                          :tx (raw "nextval('tx')"))]
                          :returning [:*]})))




(comment
  (jdbc/query db "select 1")

  (q db {:select [:*] :from [:test]})
  (q db {:select [:*] :from [:rooms]})

  (exec db "drop table  if exists test; create table test (id serial, resource jsonb)")

  (q db
     {:insert-into :test
      :values [{:resource (json/generate-string {:a 1})}]
      :returning [:*]})


  (jdbc/query db "select * from information_schema.tables limit 10")

  (q db {:select [(honey/raw "resource#>>'{name,given}'")]
         :from [:test]
         :where [:like [(honey/raw "resource#>>'{name,given}'")] "%a"]}))
