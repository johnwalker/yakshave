(ns io.johnwalker.yakshave.new
  (:require [clojure.set]
            [io.johnwalker.yakshave.new.builtins.lein :as lein]
            [io.johnwalker.yakshave.new.templates :as t]
            [io.johnwalker.yakshave.node :as node]))

(defn standard-map [template project]
  (let [standard {:year        (t/year)
                  :full-name   project
                  :sanitized   (t/sanitize-ns project)
                  :main-ns     (t/multi-segment project)
                  :path        (t/name-to-path project)
                  :group-id    (t/group-name project)
                  :artifact-id (t/project-name project)}]
    standard))

(defn load-template-jar [external]
  (let [;; an exception for leiningen
        jar-path           (case external
                             "leiningen" (t/get-mvn-path
                                          (str "leiningen/leiningen"))
                             (t/get-mvn-path external))
        versions           (filter #(re-matches #"\d\.\d\.\d" %)
                                   (node/read-dir jar-path))
        latest-version     (t/latest-version versions)
        jar-path           (t/get-jar-path jar-path external latest-version)]
    (node/load-jar jar-path)))

(defn has-yakshave? [zip]
  (seq (filter #(= "yakshave.edn" %) (.getEntries zip))))

(defn extra-lein-data [template project]
  (let [unprefixed (if (and (= template "plugin")
                            (re-matches #"lein-" project))
                     (subs project 5)
                     project)]
    {:name project
     :unprefixed-name unprefixed
     :sanitized (t/sanitize unprefixed)
     :placeholder "{{:placeholder}}"}))

(defn new
  ([external template project]
     (new external template project false))
  ([external template project debug]
     (let [zip                (load-template-jar external)
           data               (standard-map template project)

           ;; A second (but temporary) exception for Leiningen
           ;; templates
           data               (if (= "leiningen" external)
                                (let [renamed (clojure.set/rename-keys
                                               data
                                               {:path :nested-dirs
                                                :full-name :raw-name
                                                :main-ns :namespace})]
                                  (if (#{"plugin" "template"} template)
                                    (merge renamed (extra-lein-data template project))
                                    renamed))
                                data)
           yakshave-map           (if (has-yakshave? zip)
                                    (cljs.reader/read-string
                                     (node/render (.readAsText zip "yakshave.edn")
                                                  data))
                                    (if-let [tmp (case template
                                                   "app" lein/app
                                                   "default" lein/default
                                                   "plugin" lein/plugin
                                                   "template" lein/template
                                                   nil)]
                                      (cljs.reader/read-string
                                       (node/render tmp (clj->js data)))
                                      (.log js/console (str "There is no yakshave.edn in " template))))
           full-renderer-dir  (str "leiningen/new/" (:renderer yakshave-map))
           full-renderer-dir  (if (= (last full-renderer-dir) \/)
                                full-renderer-dir
                                (str full-renderer-dir "/"))
           datajs (clj->js data)]
       (doseq [[endpoint mustache-file] (:files yakshave-map)
               :let [endpoint (str (:artifact-id data) "/" endpoint)]]
         (if debug
           (.log js/console "mkdir-for: " endpoint)
           (node/mkdir-for endpoint))
         (let [rendered (node/render (.readAsText zip (str full-renderer-dir mustache-file))
                                     datajs)
               cleaned (t/fix-line-separators rendered)]
           (if debug
             (.log js/console cleaned)
             (.writeFileSync node/fs endpoint cleaned #js {:flag "wx"})))))))
