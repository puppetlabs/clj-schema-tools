(defproject puppetlabs/schema-tools "0.1.0-SNAPSHOT"
  :description "Tools for working with prismatic schema"
  :url "http://github.com/puppetlabs/clj-schema-tools"
  :pedantic? :abort

  :dependencies [[org.clojure/clojure "1.6.0"]]

  :deploy-repositories [["releases" {:url "https://clojars.org/repo"
                                     :username :env/clojars_jenkins_username
                                     :password :env/clojars_jenkins_password
                                     :sign-releases false}]]

  :lein-release {:scm :git
                 :deploy-via :lein-deploy}

  :plugins [[lein-release "1.0.5"]])
