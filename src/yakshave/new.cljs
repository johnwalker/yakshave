(ns yakshave.new
  (:require [clojure.set]
            [clojure.string :as str]
            [cljs.reader]
            [yakshave.new.builtins.lein :as lein]
            [yakshave.new.templates :as t]
            [yakshave.node :as node]))

(defn load-latest-jar [external]
  (let [jar-path           (case external
                             "leiningen" (t/get-mvn-path
                                          (str "leiningen/leiningen"))
                             (str (t/get-mvn-path external) "/lein-template"))
        versions           (filter #(re-matches #"\d\.\d\.\d" %) (node/read-dir jar-path))
        latest-version     (t/latest-version versions)
        jar-path           (case external
                             "leiningen" (t/get-jar-path jar-path external latest-version)
                             (t/get-jar-path jar-path latest-version))]
    (node/load-jar jar-path)))

(defn has-yakshave? [zip]
  (seq (filter #(= "yakshave.edn" %) (.getEntries zip))))

(defn render-yakshave [ys datajs]
  (let [error-msg (fn [x]
                    (str x " in yakshave.edn is not a vector or string."))]
    (update-in ys [:files]
               (fn [v] (mapv
                        (fn [x]
                          (cond (vector? x) [(node/render (first x) datajs)
                                             (second x)]
                                (string? x) (node/render x datajs)
                                :else (throw (js/Error. (error-msg x)))))
                        v)))))

(defn raw-yakshave-map [template zip]
  (case template
    "app" lein/app
    "default" lein/default
    "plugin" lein/plugin
    "template" lein/template
    (let [raw-text
          (try
            (.readAsText zip "yakshave.edn")
            (catch js/Error e
              (throw (js/Error.
                      (str template " doesn't contain a yakshave.edn.")))))
          yakshave
          (try
            (cljs.reader/read-string raw-text)
            (catch js/Error e
              (throw (js/Error. (str "The yakshave.edn is malformed.")))))]
      yakshave)))

(defn unprefix [s]
  (if (re-matches #"lein-*" s)
    (subs s 5)
    s))

(def function-map
  {:year            (constantly t/year)
   :identity        identity
   :sanitize-ns     t/sanitize-ns
   :sanitize        t/sanitize
   :multi-segment   t/multi-segment
   :name-to-path    t/name-to-path
   :group-name      t/group-name
   :project-name    t/project-name
   :unprefix        unprefix})

(defn build-data [ys project]
  (into {}
        (map
         (fn [[k v]]
           (if (vector? v)
             [k (reduce (fn [x f]
                          (try ((f function-map) x)
                               (catch js/Error e
                                 (throw (js/Error. (str f " is not a built-in function.")))))) project v)]
             [k v])))
        (:data ys)))

(defn new
  ([external template project]
     (let [zip                (load-latest-jar external)
           yakshave-map       (raw-yakshave-map template zip)
           artifact-id        (t/project-name project)
           data               (build-data yakshave-map project)
           datajs             (clj->js data)
           yakshave-map       (render-yakshave yakshave-map datajs)
           full-renderer-dir  (str "leiningen/new/" (t/sanitize (:renderer yakshave-map)))
           full-renderer-dir  (if (= (last full-renderer-dir) \/)
                                full-renderer-dir
                                (str full-renderer-dir "/"))]
       (doseq [entry (:files yakshave-map)]
         (cond (string? entry)
               (let [dir (str artifact-id "/" entry)]
                 (node/mkdirp dir #js {} identity))
               (vector? entry)
               (let [[endpoint mustache-file] entry
                     endpoint (str artifact-id "/" endpoint)
                     dir      (apply str (interpose "/" (butlast (str/split endpoint #"/"))))]
                 (node/mkdirp dir #js {}
                              (fn [e]
                                (let [rendered (node/render (.readAsText zip (str full-renderer-dir mustache-file))
                                                            datajs)
                                      cleaned (t/fix-line-separators rendered)]
                                  (.writeFile node/fs endpoint cleaned #js {:flag "wx"} identity))))))))))
