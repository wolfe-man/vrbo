(ns vrbo.email
  (:require [amazonica.aws.simpleemail :as ses]
            [hiccup.table :as ht]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [include-css
                                 include-js]]
            [vrbo.config :refer [with-s3-credentials
                                 config]]))


(defn deliver-email [table]
  (let [table-html
        (html [:head
               (include-css (str "https://maxcdn.bootstrapcdn.com"
                                 "/bootstrap/3.3.7/css/bootstrap.min.css"))
               (include-js (str "https://ajax.googleapis.com"
                                "/ajax/libs/jquery/3.1.1/jquery.min.js"))
               (include-js (str "https://maxcdn.bootstrapcdn.com"
                                "/bootstrap/3.3.7/js/bootstrap.min.js"))
               [:body table]])]
    (with-s3-credentials config
      (ses/send-email :destination {:to-addresses [(:email config)]}
                      :source (:email config)
                      :message {:subject "Daily VRBO Position Alert"
                                :body {:html table-html}}))))


(defn generate-table [listings]
  (let [lookup-hyperlinks (reduce (fn [coll {:keys [unit search-page page]}]
                                    (let [pg (if (= page "Listing not found")
                                               1
                                               page)
                                          listing-page (str search-page
                                                            "?page=" pg)]
                                      (merge coll {unit listing-page})))
                                  {} listings)
        attr-fns {:table-attrs {:class "table table-striped table-bordered"}
                  :thead-attrs {:id "thead"}
                  :tbody-attrs {:id "tbody"}
                  :data-tr-attrs {:class "trattrs"}
                  :th-attrs (fn [label-key _] {:class (name label-key)})
                  :data-td-attrs
                  (fn [label-key val]
                    (case label-key
                      :height (if (<= 180 val)
                                {:class "above-avg"}
                                {:class "below-avg"}) nil))
                  :data-value-transform
                  (fn [label-key val]
                    (if (= :unit label-key)
                      [:a {:href (get lookup-hyperlinks val)} val]
                      val))}]
    (ht/to-table1d listings
                   [:date "Date" :listing-id "Listing ID"
                    :unit "Unit" :page "Page" :position "Position"
                    :overall-position "Overall Position"]
                   attr-fns)))


(defn email-listings [listings]
  (->> listings
       generate-table
       deliver-email))
