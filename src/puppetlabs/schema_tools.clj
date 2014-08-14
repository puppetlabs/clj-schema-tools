(ns puppetlabs.schema-tools
  (:require [clojure.walk :refer [postwalk prewalk]]
            [clojure.string :as string]
            [schema.core :as sc]
            [schema.utils :refer [named-error-explain validation-error-explain]]))

(defn- has-schema-error-classes?
  [x]
  (let [!has? (atom false)
        trip-on-schema-error (fn [x]
                               (let [c (class x)]
                                 (when (or (= c schema.utils.ValidationError)
                                           (= c schema.utils.NamedError))
                                   (reset! !has? true))
                                 x))]
    (prewalk trip-on-schema-error x)
    @!has?))

(defn- explain-schema-errors
  [x]
  (let [explainer (fn [x]
                    (condp = (class x)
                      schema.utils.ValidationError (validation-error-explain x)
                      schema.utils.NamedError (named-error-explain x)
                      x))
        explained (postwalk explainer x)]
    (if (has-schema-error-classes? explained)
      (recur explained)
      explained)))

(defn- ->client-explanation
  "Transforms a schema explanation into one that makes more sense to a
  javascript client by removing any java.lang. prefixes and changing any
  Keyword symbols to String."
  [explanation]
  (let [explained (explain-schema-errors explanation)
        class->sym (fn [x]
                     (if (class? x)
                       (symbol (.getCanonicalName x))
                       x))
        strip-sym-prefix (fn [x]
                           (let [xstr (str x)]
                             (cond
                               (not (symbol? x)) x
                               (not (re-find #"^java\.lang\." xstr)) x
                               :else (symbol (string/replace xstr "java.lang." "")))))
        var->string (fn [x]
                      (if (var? x)
                        (str x)
                        x))
        fn->string (fn [x]
                     (if (fn? x)
                       (-> (str x) (string/replace #"@.*$" ""))
                       x))]
    (postwalk (comp var->string fn->string strip-sym-prefix class->sym)
              explained)))

(defn- dissoc-indices
  [v & is]
  (let [remove-index? (set is)]
    (->> (map vector v (range))
      (remove (comp remove-index? second))
      (map first)
      vec)))

(defn- remove-passing-arguments
  [{:keys [schema value error] :as data}]
  (if-not (sequential? error)
    data
    (let [passing-arg-indices (->> (map vector error (range))
                                (filter (comp nil? first))
                                (map second))
          remove-passing #(apply dissoc-indices % passing-arg-indices)]
      {:schema (if (= (count schema) (count error))
                 (remove-passing schema)
                 schema)
       :value (remove-passing value)
       :error (remove-passing error)})))

(defn- remove-credentialed-arguments
  [{:keys [schema value error] :as data}]
  (if-not (sequential? value)
    data
    (let [has-credentials? #(and (associative? %)
                                 (contains? % :db)
                                 (associative? (:db %))
                                 (contains? (:db %) :user)
                                 (contains? (:db %) :password))
          credentialed-arg-indices (->> (map vector value (range))
                                     (filter has-credentials?)
                                     (map second))
          remove-creds #(apply dissoc-indices % credentialed-arg-indices)]
      {:schema (remove-creds schema)
       :value (remove-creds value)
       :error (remove-creds error)})))

(defn- unwrap-if-length-one
  [x]
  (if (and (sequential? x) (= (count x) 1))
    (first x)
    x))

(defn- remove-argument-annotations
  [{:keys [schema value error]}]
  (let [select-schema-or-error (fn [v]
                                 (if-not (sequential? v)
                                   v
                                   (nth v 1)))]
    {:value (unwrap-if-length-one value)
     :schema (->> schema
               (map select-schema-or-error)
               unwrap-if-length-one)
     :error (->> error
              (map select-schema-or-error)
              unwrap-if-length-one)}))

(defn- simplify-argument-schema-exception-data
  "Simplifies the schema, value, and error exception data from a schema
  validation exception so that exceptions from schema.core/defn's argument
  schemas don't contain any extraneous information about arguments that didn't
  actually fail validation. It does this by looking at the error field and
  noting the indices where nil appears (signifying that that argument doesn't
  fail), and removing the values at those indices from all three exception
  data fields. Then, if only 1-vecs remain after the removal, their sole
  remaining value will be unwrapped.

  This prevents us leaking database credentials through validation exceptions
  from the postgres storage protocol function implementations, since the first
  argument of those always contains the credentials, but there is no schema
  for that argument so it will never fail to validate."
  [data]
  (-> data
    remove-passing-arguments
    remove-credentialed-arguments
    remove-argument-annotations))

(defn- explain-schema-exception-data
  [{:keys [value schema error]}]
  {:value value
   :schema (-> schema sc/explain ->client-explanation)
   :error (->client-explanation error)})

(defn explain-and-simplify-exception-data
  "Turn the schema, value, and error data attached to a schema exception into
  their simplified explanations with any credential values removed, ready to
  be serialized to JSON."
  [{:keys [schema] :as data}]
  (let [explained (explain-schema-exception-data data)
        {schema-exp :schema
         error-exp :error
         value :value} (if (sequential? schema)
                         (simplify-argument-schema-exception-data explained)
                         explained)]
    {:schema schema-exp
     :value value
     :error error-exp}))
