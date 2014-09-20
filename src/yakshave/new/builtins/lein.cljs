(ns yakshave.new.builtins.lein)

(def app
  {:renderer "app"
   :files [["project.clj" "project.clj"]
           ["README.md" "README.md"]
           ["doc/intro.md" "intro.md"]
           [".gitignore" "gitignore"]
           ["src/{{nested-dirs}}.clj" "core.clj"]
           ["test/{{nested-dirs}}_test.clj" "test.clj"]
           ["LICENSE" "LICENSE"]
           "resources"]
   :data {:raw-name    [:identity]
          :name        [:project-name]
          :namespace   [:sanitize-ns
                        :multi-segment]
          :nested-dirs [:sanitize-ns
                        :multi-segment
                        :name-to-path]
          :year        [:year]}})

(def default
  {:renderer "default"
   :files [["project.clj" "project.clj"]
           ["README.md" "README.md"]
           ["doc/intro.md" "intro.md"]
           [".gitignore" "gitignore"]
           ["src/{{nested-dirs}}.clj" "core.clj"]
           ["test/{{nested-dirs}}_test.clj" "test.clj"]
           ["LICENSE" "LICENSE"]
           "resources"]
   :data {:raw-name    [:identity]
          :name        [:project-name]
          :namespace   [:sanitize-ns :multi-segment]
          :nested-dirs [:sanitize-ns :multi-segment :name-to-path]
          :year        [:year]}})

(def plugin
  {:renderer "plugin"
   :files [["project.clj" "project.clj"]
           ["README.md" "README.md"]
           [".gitignore" "gitignore"]
           ["src/leiningen/{{sanitized}}.clj" "name.clj"]
           ["LICENSE" "LICENSE"]]
   :data {:name            [:identity]
          :unprefixed-name [:unprefix]
          :sanitized       [:unprefix :sanitize]
          :year            [:year]}})

(def template
  {:renderer "template"
   :files [["README.md" "README.md"]
           ["project.clj" "project.clj"]
           [".gitignore" "gitignore"]
           ["src/leiningen/new/{{sanitized}}.clj" "temp.clj"]
           ["resources/leiningen/new/{{sanitized}}/foo.clj" "foo.clj"]
           ["LICENSE" "LICENSE"]]
   :data {:name        [:identity]
          :sanitized   [:sanitize]
          :placeholder "{{:sanitized}}"
          :year        [:year]}})
