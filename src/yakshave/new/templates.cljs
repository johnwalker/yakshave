(ns yakshave.new.templates
  (:require [clojure.string :as str]
            [yakshave.node :refer [maven-repository-home os path mustache]]))

;; This is a Clojurescript copy of leiningen.new.templates.

;; At the time we brought leiningen.new.templates to Clojurescript,
;; the its 12 contributors were:

;; _hyPIRion _technomancy _cldwalker _Raynes
;; _kumarshantanu _mpblair _joelittlejohn _josephwilk
;; _CraZySacX _malyn _trptcolin _timmc

;; Thanks guys!

;; https://github.com/technomancy/leiningen/blob/master/src/leiningen/new/templates.clj

(defn latest-version [versions]
  (->> versions
       (map #(str/split % #"\."))
       (sort >)
       first
       (interpose ".")
       (apply str)))

(defn directify [& strs]
  (apply str (interpose "/" strs)))

(defn get-mvn-path
  [& args]
  (apply directify maven-repository-home args))

(defn get-jar-path
  ([path version]
     (directify path version (str "lein-template-"
                                  version
                                  ".jar")))
  ([path name version]
     (directify path version (str name "-"
                                  version
                                  ".jar"))))

(defn project-name
  "Returns project name from (possibly group-qualified) name:
  mygroup/myproj => myproj
  myproj => myproj"
  [s]
  (last (str/split s #"/")))

(defn fix-line-separators
  "Replace all \\n with system specific line separators."
  [s]
  (if (= "\n" (.-EOL os))
    (str/replace s "\n" (.-EOL os))
    s))

(defn sanitize
  "Replace hyphens with underscores."
  [s]
  (str/replace s "-" "_"))

(defn multi-segment
  "Make a namespace multi-segmented by adding another segment if necessary.
  The additional segment defaults to \"core\"."
  ([s] (multi-segment s "core"))
  ([s final-segment]
     (if (re-matches #"\." s)
       s
       (str s "." final-segment))))

(defn name-to-path
  "Constructs directory structure from fully qualified artifact name:
  \"foo-bar.baz\" becomes \"foo_bar/baz\"
  and so on."
  [s]
  (-> s sanitize (str/replace "." "/")))

(defn sanitize-ns
  "Returns project namespace name from (possibly group-qualified) project name:
  mygroup/myproj => mygroup.myproj
  myproj => myproj
  mygroup/my_proj => mygroup.my-proj"
  [s]
  (-> s
      (str/replace "/" ".")
      (str/replace "_" "-")))

(defn group-name
  "Returns group name from (a possibly unqualified) name:
  my.long.group/myproj => my.long.group
  mygroup/myproj => mygroup
  myproj => nil"
  [s]
  (let [grpseq (butlast (str/split (sanitize-ns s) #"\."))]
    (if (seq grpseq)
      (->> grpseq (interpose ".") (apply str)))))

(defn year
  "Get the current year. Useful for setting copyright years and such."
  [] (.getFullYear (js/Date.)))

(defn render-text [s data] (.render mustache s data))

(defn- template-path [name path data]
  (str name (render-text path data)))

(def ^{:dynamic true} *dir* nil)

(def ^{:dynamic true} *force?* false)
