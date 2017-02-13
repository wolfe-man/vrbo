(ns vrbo.config
  (:require [amazonica.aws.s3 :as s3]
            [environ.core :refer [env]]))


(def config
  {:access-key (:access-key env)
   :secret-key (:secret-key env)
   :bucket-name (:s3-bucket env)
   :email (:email env)})


(def dynamo-config (merge {:endpoint (:dynamodb-endpoint env)}
                          config))


(def s3-config (merge {:endpoint (:s3-endpoint env)}
                      config))


(defmacro with-aws-credentials
  [credentials [aws-func & args]]
  `(let [updated-args# (if (and (:access-key ~credentials)
                                (:secret-key ~credentials))
                         (cons ~credentials (list ~@args))
                         (list ~@args))]
     (apply ~aws-func updated-args#)))
