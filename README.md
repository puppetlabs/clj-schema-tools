# schema-tools

A Clojure library for working with prismatic/schema

There is currently only one public function:
`explain-and-simplify-exception-data`. It cleans up the data attached to
a schema exception to make it more presentable over a JSON API. Given a
map with keys `:schema`, `:value`, and `:error`, representing the schema,
the value to be validated against the schema, and the validation error
respectively, it returns a map of the same structure with the processed
versions of these data.

A number of transformations are applied. Since a schema validation
errors will include data about all arguments of a function, any data
about arguments that did not fail validation will be removed. Arguments
that look like database specs for use with jdbc are cleaned to prevent
leaking credentials. Standard java types have `java.lang` removed, e.g.
`java.lang.String` becomes `String`. Any other elements of the
validation error that would not normally be coercable to JSON are
replaced with a string representation.
