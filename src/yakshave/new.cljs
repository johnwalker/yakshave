(ns yakshave.new
  (:require [clojure.set]
            [clojure.string :as str]
            [cljs.reader]
            [yakshave.new.builtins.lein :as lein]
            [yakshave.new.templates :as t]
            [yakshave.node :as node]))

(defn standard-map [project]
  {:year         (t/year)
   :name          project
   :sanitized-ns (t/sanitize-ns project)
   :main-ns      (t/multi-segment project)
   :path         (t/name-to-path (t/multi-segment project))
   :group-id     (t/group-name project)
   :artifact-id  (t/project-name project)})

(defn leiningen-map [template project]
  (let [unprefixed (if (and (= template "plugin")
                            (re-matches #"lein-" project))
                     (subs project 5)
                     project)]
    {:name project
     :unprefixed-name unprefixed
     :sanitized (t/sanitize unprefixed)
     :placeholder "{{:placeholder}}"}))

(defn compromise-map [external template project]
  (let [standard (standard-map project)]
    (if (= "leiningen" external)
      (let [a-compromise (clojure.set/rename-keys
                          standard
                          {:path :nested-dirs
                           :full-name :raw-name
                           :main-ns :namespace})]
        (merge a-compromise (leiningen-map template project)))
      standard)))

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

(defn yakshave-map [template zip datajs]
  (let [builtin-fn #(update-in % [:files]
                               (fn [v] (mapv
                                        (fn [x]
                                          (cond (vector? x) [(node/render (first x) datajs) (second x)]
                                                (string? x) (node/render x datajs)
                                                :else :shit))
                                        v)))]
    (case template
      "app" (builtin-fn lein/app)
      "default" (builtin-fn lein/default)
      "plugin" (builtin-fn lein/plugin)
      "template" (builtin-fn lein/template)
      (try
        (cljs.reader/read-string
         (node/render (.readAsText zip "yakshave.edn") datajs))
        (catch js/Error e
            (throw (js/Error. (str "The " template " template doesn't have a yakshave.edn"))))))))

(defn new
  ([external template project]
     (let [zip                (load-latest-jar external)
           data               (compromise-map external template project)
           datajs             (clj->js data)
           yakshave-map       (yakshave-map template zip datajs)
           full-renderer-dir  (str "leiningen/new/" (:renderer yakshave-map))
           full-renderer-dir  (if (= (last full-renderer-dir) \/)
                                full-renderer-dir
                                (str full-renderer-dir "/"))]
       (let [artifact-id (:artifact-id data)]
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
                                    (.writeFile node/fs endpoint cleaned #js {:flag "wx"} identity)))))))))))
