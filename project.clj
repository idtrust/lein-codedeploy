(defproject ystad/aws-codedeploy "1.0.0"
  :description "A leiningen plugin to create a codedeploy archive"

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[clj-yaml "0.4.0"]
                 [commons-io/commons-io "2.4"]
                 [stencil "0.3.5"
                  :exclusions [org.clojure/core.cache]]]

  :eval-in-leiningen true)
