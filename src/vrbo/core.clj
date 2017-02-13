(ns vrbo.core
  (:gen-class
   :implements [com.amazonaws.services.lambda.runtime.RequestStreamHandler])
  (:require [vrbo.email :refer [email-listings]]
            [vrbo.extract :refer [extract-s3-obj]]
            [vrbo.transform :refer [transform-listings]]
            [vrbo.upload :refer [upload-listings]]))


(defn execute []
  (let [xlsx-listings (extract-s3-obj)
        vrbo-listings (transform-listings xlsx-listings)]
    (dorun (upload-listings vrbo-listings))
    (dorun (email-listings vrbo-listings))))


(defn -handleRequest
  [this input output context]
  (execute))
