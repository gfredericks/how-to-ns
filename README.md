# how-to-ns

how-to-ns is a Clojure linter for
[Stuart Sierra's how-to-ns standard](https://stuartsierra.com/2016/clojure-how-to-ns.html).

So far it is somewhat hacky and only has the particular features that
I want or that haven't been difficult to add.

## Obtention

**NOTE!** As of version `0.2.*`, how-to-ns has been split into the
library `how-to-ns` and the leiningen plugin `lein-how-to-ns`.

See the next two sections for respective maven coordinates.

## Leiningen Usage

Add `[com.gfredericks/lein-how-to-ns "0.2.3"]` to the `:plugins` vector
of your project.clj or `:user` profile.

To lint the ns forms, printing diffs wherever there are problems:
```
lein how-to-ns check
```

To fix the ns forms that don't pass the linter:
```
lein how-to-ns fix
```

## Library Usage

Maven coordinates: `[com.gfredericks/how-to-ns "0.2.3"]`

``` clojure
(require '[com.gfredericks.how-to-ns :as how-to-ns])

;; see below for description of opts
(how-to-ns/good-ns-str?               ns-str opts)
(how-to-ns/format-ns-str              ns-str opts)
(how-to-ns/starts-with-good-ns-str? file-str opts)
(how-to-ns/format-initial-ns-str    file-str opts)
```

## Customization

Either via the `opts` param in the library or a `:how-to-ns` entry in
your `project.clj` or `:user` profile for the lein plugin, the
following options are available, shown here with their default values:

``` clojure
{:require-docstring?      true
 :sort-clauses?           true
 :allow-refer-all?        false
 :allow-rename?           false
 :allow-extra-clauses?    false
 :align-clauses?          false
 :import-square-brackets? false}
```

## Things it doesn't do until somebody makes it do them

- preserve comments or any other irregular whitespace
- support cljc
  - clojure's reader supports this via `{:read-cond :preserve}`, but we would also
    need to know what formatting to target, especially since a conditional can show
    up at any level
    - one idea is to only allow them as direct children of the `:require`/etc forms

## License

Copyright © 2016 Gary Fredericks

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
