(defproject vrbo "0.4.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[amazonica "0.3.80"
                  :exclusions [com.amazonaws/aws-java-sdk
                               com.amazonaws/amazon-kinesis-client]]
                 [clj-time "0.13.0"]
                 [com.amazonaws/aws-lambda-java-core "1.1.0"]
                 [com.amazonaws/aws-java-sdk-core "1.11.75"]
                 [com.amazonaws/aws-java-sdk-dynamodb "1.11.75"]
                 [com.amazonaws/aws-java-sdk-s3 "1.11.82"]
                 [com.amazonaws/aws-java-sdk-ses "1.11.82"]
                 [com.draines/postal "2.0.2"]
                 [dk.ative/docjure "1.11.0"]
                 [environ "1.1.0"]
                 [hiccup "1.0.5"]
                 [hiccup-table "0.2.0"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/core.memoize "0.5.8"]
                 [org.clojure/data.csv "0.1.3"]
                 [org.clojure/data.json "0.2.6"]
                 [org.flatland/ordered "1.5.4"]]
  :plugins [[lein-ancient "0.6.10"]
            [lein-bikeshed "0.4.1"]
            [lein-kibit "0.1.3"]]
  :profiles {:uberjar {:aot :all}})
