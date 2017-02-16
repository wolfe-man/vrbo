(ns vrbo.upload
  (:require [amazonica.aws.dynamodbv2 :as db]
            [vrbo.config :refer [dynamo-config
                                 with-aws-credentials]]))


(defn dynamo-upload [listings-group]
  (with-aws-credentials dynamo-config
    (db/batch-write-item :return-consumed-capacity "TOTAL"
                         :return-item-collection-metrics "SIZE"
                         :request-items
                         {"vrbo-listings" listings-group})))


(defn transform-dynamo-upload [listings]
  (dynamo-upload (reduce (fn [coll m]
                           (conj coll {:put-request {:item m}}))
                         [] listings)))


(defn upload-listings [listings]
  (->> listings
       (partition-all 20)
       (map transform-dynamo-upload)))
