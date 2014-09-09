(ns io.johnwalker.yakshave.node
  (:require [clojure.string :as str]
            [cljs.nodejs :as nodejs]))

(nodejs/enable-util-print!)

(def os       (nodejs/require "os"))
(def path     (nodejs/require "path"))
(def fs       (nodejs/require "fs"))
(def adm      (nodejs/require "adm-zip"))
(def mustache (nodejs/require "mustache"))
(set! mustache.escape identity)

(def maven-repository-home
  (or (.-M2_REPO (.-env nodejs/process))
      (str (let [home (.-HOME (.-env nodejs/process))]
             (if (= "/" (last home))
               home
               (str home "/")))
           ".m2/repository")))

(defn render [s js-m]
  (.render mustache s js-m))

(defn cwd []
  (.cwd nodejs/process))

(defn load-jar [path]
  (adm path))

(defn jar-read [f]
  (.readAsText adm f))

(defn read-dir [path]
  (.readdirSync fs path))

(defn mkdir [path]
  (.mkdirSync fs path))

(defn mkdir-for [filepath]
  (let [dirs (str/split filepath #"/")]
    (reduce
     (fn [current-dir dir]
       (try
         (mkdir current-dir)
         (catch js/Error e
           ;; TODO: delete this
           nil))
       (str current-dir "/" dir))
     (first dirs) (rest dirs))))
