(ns vrbo.transform
  (:require [clj-time.local :as l]
            [clj-time.format :as f]
            [clojure.core.memoize :as memo]
            [clojure.data.json :as json]))

;; need to enrch new columns
(defn date-yyyy-MM-dd []
  (f/unparse (f/formatter "yyyy-MM-dd") (l/local-now)))


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
                                 ((partial re-find #"(?<=VRBO.indexMaplisings = )(.*)(?=;)")))]
                 (-> result
                     first
                     (json/read-str :key-fn keyword)))))))


(def parse-vrbo-listing
  (memo/lru
   (fn [search-page-pg]
     (-> search-page-pg
         execute-get-json
         :listings
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
  (loop [pg 1]
    (let [listings (get-vrbo-listings listing-map pg)
          pos (when listings
                (-> listings
                    (.indexOf listing-id)
                    inc))] ;;inc because 0 position is actually 1
      (cond (empty? listings)
            {:position "Listing not found"
             :overall-position "Listing not found"
             :page "Listing not found"}
            (pos? pos)
            {:position pos
             :overall-position (overall-pos pos pg)
             :page pg}
            :else
            (recur (inc pg))))))


(defn enrich-vrbo-listings [{listing-id :listing-id :as listing-map}]
  (let [position-and-page (get-position-and-page listing-map)]
    (merge {:date (date-yyyy-MM-dd)}
           listing-map
           position-and-page)))


(defn transform-listings [listings]
  (map enrich-vrbo-listings listings))
