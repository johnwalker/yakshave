(ns io.johnwalker.yakshave.core
  (:require-macros [cljs.core.match.macros :refer [match]])
  (:require [cljs.reader]
            [cljs.core.match]
            [io.johnwalker.yakshave.new :as task]))

(defn -main [& args]
  (let [debug (= ":debug" (first args))
        args (if debug
               (rest args)
               args)
        vargs (vec args)]
    (match [vargs]
           [["new"
             project-name]]
           (task/new "leiningen" "default" project-name debug)

           [["new"
             (template :guard #{"app" "template" "plugin" "default"})
             project-name]]
           (task/new "leiningen" template project-name debug)

           [["new" template project-name]]
           (task/new template    template project-name debug))))

(set! *main-cli-fn* -main)
