(ns app.db
  (:require [honeysql.core :as honey]
            [honeysql.format :as sqlf]
            [clojure.java.jdbc :as jdbc]))

;; use java

(def db {:dbtype "postgresql"
         :connection-uri "jdbc:postgresql://localhost:5679/postgres?stringtype=unspecified&user=postgres&password=secret"})

(jdbc/query db "select 1")


(jdbc/query
 db
 "select *
  from information_schema.tables
  limit 10")


;; honey

(def tables-q
  {:select [:*]
   :from   [:information_schema.tables]
   :limit  10})

(honey/format tables-q)

(jdbc/query db (honey/format tables-q))

;; ;; composabiity

(defn with-catalog [q catalog]
  (assoc q :where [:= :table_schema catalog]))

(with-catalog {} "public")

(defn with-name [q nm]
  (let [cr [:like :table_name (str "%" nm "%")]]
    (->> (if-let [where (:where q)]
           [:and cr where]
           cr)
         (assoc q :where))))

(with-name {} "nik")

;; (-> tables-q
;;     (with-name "tabl"))

(defn tables []
  (->> (-> tables-q
       (with-catalog "information_schema")
       (with-name "tabl"))
       (honey/format)
       (jdbc/query db)
       (mapv :table_name)))

(tables)

(defn where-and [q cr]
  (->> (if-let [where (:where q)]
         [:and cr where]
         cr)
       (assoc q :where)))

(defn with-catalog [q catalog]
  (where-and q [:= :table_schema catalog]))

(defn with-name [q nm]
  (where-and q [:like :table_name (str "%" nm "%")]))

(-> tables-q
    (with-name "tabl")
    (with-catalog "information_schema")
    (honey/format)
    (->>  (jdbc/query db)))

(jdbc/execute! db "drop table  if exists test; create table test (id serial, resource jsonb)")

(jdbc/query db "select * from test")

(honey/format
 {:insert-into :test
  :values {:resource "{\"name\": \"Nikolai\"}"}
  :returning [:*] ;; this does not work!
  })


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

(require '[cheshire.core :as json])

(jdbc/query db
 (honey/format
  {:insert-into :test
   :values [{:resource
             (json/generate-string {:name {:given "Nicola"
                                           :family "Ryzhikov"}})}]
   :returning [:*]}))

(defn q [db hsql]
  (jdbc/query db (if (string? hsql) hsql (honey/format hsql))))



(require '[clojure.test :as t])

(t/is (= (q db {:select [:*] :from [:test]})
           []))

(-> (q db {:select [:*] :from [:test]})
    first
    :resource
    type)

;; (-> (q db {:select [:*] :from [:test]})
;;     type)

(import '[org.postgresql.util PGobject])

(defmulti coerse (fn [x] (type x)))

(defmethod coerse :default [x] x)

(coerse 1)

(coerse "ups")

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

(type {:a 1})

(defmethod coerse
  clojure.lang.PersistentArrayMap
  [h]
  (reduce (fn [acc [k v]] (assoc acc k (coerse v))) {} h))

(defn q [db hsql]
  (->> (jdbc/query db (if (string? hsql) hsql (honey/format hsql)))
       (mapv coerse)))

(q db {:select [:*] :from [:test]})

(q db "delete from test returning *")

(q db {:select [(honey/raw "resource#>>'{name,given}'")]
       :from [:test]})

(q db {:select [(honey/raw "resource#>>'{name,given}'")]
       :from [:test]
       :where [:like [(honey/raw "resource#>>'{name,given}'")] "%a"]})



