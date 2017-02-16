(ns vrbo.extract
  (:require [amazonica.aws.s3 :as s3]
            [dk.ative.docjure.spreadsheet :as doc]
            [vrbo.config :refer [s3-config
                                 with-aws-credentials]]))


(defn extract-obj [obj-key]
  (:input-stream (with-aws-credentials s3-config
                   (s3/get-object
                    :bucket-name (:bucket-name s3-config)
                    :key obj-key))))


(defn extract-s3-obj []
  (let [obj-key "VRBO_Search_IDs.xlsx"]
    (->> obj-key
         extract-obj
         doc/load-workbook
         (doc/select-sheet "excel_marketing_complex")
         (doc/select-columns {:A :unit :B :listing-id :C :search-page})
         rest
         (map #(update % :listing-id (comp str int))))))
