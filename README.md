# how-to-ns

how-to-ns is a Clojure linter (as a Leiningen plugin) for
[Stuart Sierra's how-to-ns standard](https://stuartsierra.com/2016/clojure-how-to-ns.html).

So far it is somewhat hacky and only has the particular features that
I want.

## Obtention

Add `[com.gfredericks/how-to-ns "0.1.7"]` to the `:plugins` vector
of your project.clj or `:user` profile.

## Usage

To lint the ns forms, printing diffs wherever there are problems:
```
lein how-to-ns check
```

To fix the ns forms that don't pass the linter:
```
lein how-to-ns fix
```

## Customization

Add a `:how-to-ns` entry (to `project.clj` or the `:user` profile) with
any of the following options:

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

## License

Copyright © 2016 Gary Fredericks

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
