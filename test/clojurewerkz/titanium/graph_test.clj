(ns clojurewerkz.titanium.graph-test
  (:require [clojurewerkz.titanium.graph    :as tg]
            [clojurewerkz.titanium.vertices :as tv]
            [clojurewerkz.titanium.edges    :as ted]
            [clojurewerkz.titanium.schema   :as ts]
            [clojurewerkz.support.io        :as sio]
            [clojurewerkz.ogre.graph  :as c])
  (:use clojure.test
        [clojurewerkz.titanium.test.support :only (graph-fixture *graph*)])
  (:import org.apache.commons.io.FileUtils
           (com.thinkaurelius.titan.graphdb.vertices StandardVertex)
           (com.thinkaurelius.titan.graphdb.database StandardTitanGraph)
           [java.util.concurrent CountDownLatch TimeUnit]))

(use-fixtures :once graph-fixture)

(deftest open-and-close-a-local-graph-with-a-shortcut
  (let [d (sio/create-temp-dir)]
    (is (let [graph (tg/open (str "berkeleyje:" (.getPath d)))]
          (and graph (nil? (tg/close graph)))))))

(deftest test-open-and-close-a-local-graph-with-a-connfiguration-map
  (let [d (sio/create-temp-dir)]
    (is (let [graph  (tg/open {"storage.directory" (.getPath d)
                               "storage.backend"  "berkeleyje"})]
          (and graph (nil? (tg/close graph)))))))

(deftest test-conf-graph
  (testing "Graph type"
    (is (= (type *graph*) StandardTitanGraph)))

  (testing "Vertex type"
    (tg/with-transaction [tx *graph*]
        (let [vertex (tv/create! tx)]
          (is (= StandardVertex (type vertex)))))))

(deftest graph-of-the-gods

  (testing "Configure edge and vertex properties"
    (ts/with-management-system [mgmt *graph*]
      (is (ts/make-property-key mgmt :name  String :cardinality :single))
      (is (ts/make-property-key mgmt :type  String :cardinality :single))
      (is (ts/make-property-key mgmt :times Long   :cardinality :single))
      (is (ts/make-edge-label mgmt :lives   :multiplicity :many-to-one))
      (is (ts/make-edge-label mgmt :father  :multiplicity :one-to-many))
      (is (ts/make-edge-label mgmt :mother  :multiplicity :one-to-many))
      (is (ts/make-edge-label mgmt :brother :multiplicity :many-to-many))
      (is (ts/make-edge-label mgmt :pet     :multiplicity :one-to-many))
      (is (ts/make-edge-label mgmt :battled :multiplicity :many-to-many))
      (is (ts/build-composite-index mgmt :ixName :vertex [:name] :unique? true))
      (is (ts/build-composite-index mgmt :ixType :vertex [:type]))
      (is (ts/build-composite-index mgmt :ixTimes :edge [:times]))))

  (testing "Populate graph"
    (is
     (tg/with-transaction [tx *graph*]
       (let [saturn   (tv/create! tx {:name "Saturn"   :type "titan"})
             jupiter  (tv/create! tx {:name "Jupiter"  :type "god"})
             hercules (tv/create! tx {:name "Hercules" :type "demigod"})
             alcmene  (tv/create! tx {:name "Alcmene"  :type "human"})
             neptune  (tv/create! tx {:name "Neptune"  :type "god"})
             pluto    (tv/create! tx {:name "Pluto"    :type "god"})
             sea      (tv/create! tx {:name "Sea"      :type "location"})
             sky      (tv/create! tx {:name "Sky"      :type "location"})
             tartarus (tv/create! tx {:name "Tartarus" :type "location"})
             nemean   (tv/create! tx {:name "Nemean"   :type "monster"})
             hydra    (tv/create! tx {:name "Hydra"    :type "monster"})
             cerberus (tv/create! tx {:name "Cerberus" :type "monster"})]
         (ted/connect! neptune :lives sea)
         (ted/connect! jupiter :lives sky)
         (ted/connect! pluto :lives tartarus)
         (ted/connect! jupiter :father saturn)
         (ted/connect! hercules :father jupiter)
         (ted/connect! hercules :mother alcmene)
         (ted/connect! jupiter :brother pluto)
         (ted/connect! pluto :brother jupiter)
         (ted/connect! neptune :brother pluto)
         (ted/connect! pluto :brother neptune)
         (ted/connect! jupiter :brother neptune)
         (ted/connect! neptune :brother jupiter)
         (ted/connect! cerberus :lives tartarus)
         (ted/connect! pluto :pet cerberus)
         (ted/connect! hercules :battled nemean   {:times 1})
         (ted/connect! hercules :battled hydra    {:times 2})
         (ted/connect! hercules :battled cerberus {:times 12})
         true))))

  (testing "Query graph"
    (tg/with-transaction [tx *graph*]
      (is (= #{"Jupiter" "Neptune" "Pluto"}
             (set (map (fn [v] (tv/get v :name)) (iterator-seq (tv/find-by-kv tx :type "god"))))))
      (let [jupiter (.next (tv/find-by-kv tx :name "Jupiter"))]
        (is jupiter)
        (let [lives (ted/head-vertex (.next (tv/edges-of jupiter :out :lives)))]
          (is "Sky" (tv/get lives :name))))))

  (testing "Graph variables"
    (is (= nil
           (tg/get-variable *graph* :test-var)))
    (tg/set-variable *graph* :test-var "test-value")
    (is (= "test-value"
           (tg/get-variable *graph* :test-var)))
    (tg/set-variable *graph* :test-var "test-value2") 
    (is (= "test-value2"
           (tg/get-variable *graph* :test-var)))
    (tg/remove-variable *graph* :test-var)
    (is (= nil
           (tg/get-variable *graph* :test-var)))))










