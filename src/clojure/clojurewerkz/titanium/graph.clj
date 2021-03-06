;; Copyright (c) 2013-2014 Michael S. Klishin, Alex Petrov, Zack Maril, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.titanium.graph
  (:require [potemkin :as po]
            [clojurewerkz.ogre.graph :as g])
  (:import  [com.thinkaurelius.titan.core TitanFactory TitanGraph]
            [org.apache.tinkerpop.gremlin.structure Vertex Edge
             Graph]
            [org.apache.tinkerpop.gremlin.tinkergraph.structure TinkerGraph]
            [com.thinkaurelius.titan.core TitanTransaction]))

(po/import-fn g/close)
(po/import-fn g/new-transaction)
(po/import-fn g/commit)
(po/import-fn g/rollback)
(po/import-macro g/with-transaction)
(po/import-macro g/with-transaction-retry)

;;
;; API
;;

(defn convert-config-map
  [m]
  (let [conf (org.apache.commons.configuration.BaseConfiguration.)]
    (doseq [[k1 v1] m]
      (.setProperty conf (name k1) v1))
    conf))

(defprotocol TitaniumGraph
  (open [input] "Opens a new graph"))

(extend-protocol TitaniumGraph
  String
  (open [^String shortcut-or-file]
    (TitanFactory/open shortcut-or-file))

  java.io.File
  (open [^java.io.File f]
    (TitanFactory/open (.getPath f)))

  org.apache.commons.configuration.Configuration
  (open [^org.apache.commons.configuration.Configuration conf]
    (TitanFactory/open conf))

  java.util.Map
  (open [^java.util.Map m]
    (TitanFactory/open (convert-config-map m))))

;;
;; Automatic Indexing
;;

(defn index-vertices-by-key!
  [^TinkerGraph g ^String k]
  (.createIndex g k org.apache.tinkerpop.gremlin.structure.Vertex))

(defn deindex-vertices-by-key!
  [^TinkerGraph g ^String k]
  (.dropIndex g k org.apache.tinkerpop.gremlin.structure.Vertex))

(defn index-edges-by-key!
  [^TinkerGraph g ^String k]
  (.createIndex g k org.apache.tinkerpop.gremlin.structure.Edge))

(defn deindex-edges-by-key!
  [^TinkerGraph g ^String k]
  (.dropIndex g k org.apache.tinkerpop.gremlin.structure.Edge))

;;
;; Graph Variables
;;

(defn get-variable [^Graph g ^clojure.lang.Keyword key]
  (let [variable (.get (.variables g) (name key))]
    (if (.isPresent variable)
      (.get variable)
      nil)))

(defn set-variable [^Graph g ^clojure.lang.Keyword key value]
  (.set (.variables g) (name key) value))

(defn remove-variable [^Graph g ^clojure.lang.Keyword key]
  (.remove (.variables g) (name key)))
