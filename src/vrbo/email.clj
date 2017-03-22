(ns vrbo.email
  (:require [amazonica.aws.simpleemail :as ses]
            [clj-time.local :as l]
            [clj-time.format :as f]
            [hiccup.table :as ht]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [include-css
                                 include-js]]
            [postal.core :refer [send-message]]
            [vrbo.config :refer [config]]))


(defn deliver-email [table]
  (let [table-html
        (html [:head
               (include-css (str "https://cdnjs.cloudflare.com/ajax/libs/"
                                 "twitter-bootstrap/4.0.0-alpha.6/css/"
                                 "bootstrap.css"))
               (include-css (str "https://cdn.datatables.net/1.10.13/css/"
                                 "dataTables.bootstrap4.min.css"))
               [:style ".red { color: red; }"]
               [:style ".green { color: green; }"]
               (include-js "https://code.jquery.com/jquery-1.12.4.js")
               (include-js (str "https://cdn.datatables.net/1.10.13/js/"
                                "jquery.dataTables.min.js"))
               (include-js (str "https://cdn.datatables.net/1.10.13/js/"
                                "dataTables.bootstrap4.min.js"))
               [:script (str "$(document).ready(function() { "
                             "$('#example').DataTable();"
                             "} );")]
               [:body table]])
        file-name (format "/tmp/search_rankings_report_%s.html"
                          (f/unparse (f/formatter "yyyy-MM-dd") (l/local-now)))]
    (spit file-name table-html)
    (send-message {:user (:smtp-user config)
                   :pass (:smtp-pass config)
                   :host (:smtp-host config)
                   :port 587}
                  {:from (:email config)
                   :to (:email config)
                   :subject "VRBO Search Rankings Report"
                   :body [{:type "text/html"
                           :content table-html}
                          {:type :attachment
                           :content (java.io.File. file-name)}]})))


(defn generate-table [listings]
  (let [lookup-hyperlinks (reduce (fn [coll {:keys [unit search-page page]}]
                                    (let [pg (if (= page "Listing not found")
                                               1
                                               page)
                                          listing-page (str search-page
                                                            "?page=" pg)]
                                      (merge coll {unit listing-page})))
                                  {} listings)
        attr-fns {:table-attrs {:id "example"
                                :class "table table-striped table-bordered"}
                  :thead-attrs {:id "thead"}
                  :tbody-attrs {:id "tbody"}
                  :data-tr-attrs {:class "trattrs"}
                  :th-attrs (fn [label-key _] nil)
                  :data-td-attrs
                  (fn [label-key val] nil)
                  :data-value-transform
                  (fn [label-key val]
                    (cond
                      (= :unit label-key)
                      [:a {:href (get lookup-hyperlinks val)} val]

                      (or (= :7-days-ago label-key)
                          (= :30-days-ago label-key)
                          (= :90-days-ago label-key))
                      (case (:color val)
                        "red" [:class {:class "red"} (:value val)]
                        "green" [:class {:class "green"} (str "+"(:value val))]
                        (:value val))

                      :else
                      val))}]
    (ht/to-table1d listings
                   [:date "Date" :listing-id "Listing ID"
                    :unit "Unit" :page "Page" :position "Position"
                    :overall-position "Overall Position"
                    :7-days-ago "7 Days Ago"
                    :30-days-ago "30 Days Ago"
                    :90-days-ago "90 Days Ago"]
                   attr-fns)))


(defn email-listings [listings]
  (->> listings
       generate-table
       deliver-email))
