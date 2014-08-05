(defn deploy-info
  [url]
  {:url url
   :username :env/nexus_jenkins_username
   :password :env/nexus_jenkins_password
   :sign-releases false})

(defproject puppetlabs/schema-tools "0.1.0"
  :description "Tools for working with prismatic schema"
  :url "http://github.com/puppetlabs/clj-schema-tools"
  :pedantic? :abort

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [prismatic/schema "0.2.6"]]

  :deploy-repositories [["releases" ~(deploy-info "http://nexus.delivery.puppetlabs.net/content/repositories/releases/")]
                        ["snapshots" ~(deploy-info "http://nexus.delivery.puppetlabs.net/content/repositories/snapshots/")]]

  :lein-release {:scm :git
                 :deploy-via :lein-deploy}

  :plugins [[lein-release "1.0.5"]])
