(ns vrbo.transform
  (:require [amazonica.aws.dynamodbv2 :as db]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clj-time.core :as t]
            [clojure.core.memoize :as memo]
            [clojure.data.json :as json]
            [vrbo.config :refer [dynamo-config
                                 with-aws-credentials]]))

(defn date-yyyy-MM-dd [date-time]
  (f/unparse (f/formatter "yyyy-MM-dd") date-time))


(defn date-days-ago [days-ago]
  (-> (l/local-now)
      (t/minus (t/days days-ago))
      date-yyyy-MM-dd))


(defn dynamo-get-item [date listing-id]
  (with-aws-credentials dynamo-config
    (db/get-item :table-name "vrbo-listings"
                 :key {:listing-id {:s listing-id}
                       :date {:s date}})))


(defn calc-historical-changes [listing-id overall-pos]
  (letfn [(day-chg [listing-id overall-pos days-ago]
            (if (string? overall-pos)
              "N/A"
              (->> listing-id
                   (dynamo-get-item (date-days-ago days-ago))
                   ((comp :overall-position :item))
                   ((fn [x] (if x
                              (let [chg (- x overall-pos)]
                                (cond
                                  (pos? chg) {:color "green"
                                              :value chg}
                                  (neg? chg) {:color "red"
                                              :value chg}
                                  :else {:value chg}))
                              {:value "N/A"}))))))]
    {:7-days-ago (day-chg listing-id overall-pos 7)
     :30-days-ago (day-chg listing-id overall-pos 30)
     :90-days-ago (day-chg listing-id overall-pos 90)}))


(defmacro retry
  "Evaluates expr up to cnt + 1 times, retrying if an exception
  is thrown. If an exception is thrown on the final attempt, it
  is allowed to bubble up."
  [cnt expr]
  (letfn [(go [cnt]
              (if (zero? cnt)
                expr
                `(try ~expr
                      (catch Exception e#
                        (retry ~(dec cnt) ~expr)))))]
    (go cnt)))


(defn execute-get-json [url]
  ;;(println url)
  (retry 3
         (do (Thread/sleep 1000)
             (with-open [inputstream
                         (-> url
                             java.net.URL.
                             .openConnection
                             (doto (.setRequestProperty "User-Agent"
                                                        "Mozilla/5.0 ..."))
                             .getContent)]
               (let [result (->  inputstream
                                 slurp
                                 ((partial re-find
                                           #"(?<=window.__PAGE_DATA__ = )(.*)(?=;)")))]
                 (-> result
                     first
                     (json/read-str :key-fn keyword)))))))


(def parse-vrbo-listing
  (memo/lru
   (fn [search-page-pg]
     (-> search-page-pg
         execute-get-json
         ((comp :hits :searchResults))
         ((partial map :listingNumber))))
   :lru/threshold 100))


(defn get-vrbo-listings [{:keys [listing-id search-page]}
                         pg]
  (-> search-page
      (str "?page=" pg)
      parse-vrbo-listing))


(defn overall-pos [pos pg]
  (if (> pg 1)
    (+ (* 50 (dec pg)) pos)
    pos))


(defn get-position-and-page [{listing-id :listing-id :as listing-map}]
  (loop [pg 1
         flag true]
    (if flag
      (let [listings (get-vrbo-listings listing-map pg)
            pos (when listings
                  (-> listings
                      (.indexOf (read-string listing-id))
                      inc))
            update-flag (if (< (count listings) 50)
                          false
                          true)] ;;inc because 0 position is actually 1
        (println listings)
        (if (pos? pos)
          {:position pos
           :overall-position (overall-pos pos pg)
           :page pg}
          (recur (inc pg) update-flag)))
      {:position "Listing not found"
       :overall-position "Listing not found"
       :page "Listing not found"})))


(defn enrich-vrbo-listings [{listing-id :listing-id :as listing-map}]
  (let [{overall-position :overall-position :as pos-and-page}
        (get-position-and-page listing-map)
        historical-changes (calc-historical-changes
                            listing-id
                            overall-position)]
    (merge {:date (date-yyyy-MM-dd (l/local-now))}
           listing-map
           pos-and-page
           historical-changes)))


(defn transform-listings [listings]
  (map enrich-vrbo-listings listings))
