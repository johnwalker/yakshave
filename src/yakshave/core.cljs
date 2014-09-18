(ns yakshave.core
  (:require-macros [cljs.core.match.macros :refer [match]])
  (:require [cljs.reader]
            [cljs.core.match]
            [yakshave.new :as task]))

(enable-console-print!)

(defn make-help-message [args]
  (str "Unrecognized arguments " args "\n"
       "Available tasks:\n"
       "new            ex: yakshave new foo, yakshave new app bar"))

(defn -main [& args]
  (match [(vec args)]
         [["new"
           project-name]]
         (task/new "leiningen" "default" project-name)

         [["new"
           (template :guard #{"app" "template" "plugin" "default"})
           project-name]]
         (task/new "leiningen" template project-name)

         [["new" template project-name]]
         (task/new template    template project-name)
         
         :else (.log js/console (make-help-message args))))

(set! *main-cli-fn* -main)
