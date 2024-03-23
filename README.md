# how-to-ns

how-to-ns is a Clojure linter for
[Stuart Sierra's how-to-ns standard](https://stuartsierra.com/2016/clojure-how-to-ns.html).

So far it is somewhat hacky and only has the particular features that
I want or that haven't been difficult to add.

## Obtention

**NOTE!** As of version `0.2.*`, how-to-ns has been split into the
library `how-to-ns` and the leiningen plugin `lein-how-to-ns`.

See the next two sections for respective maven coordinates.

## Usage

### Leiningen

Add `[com.gfredericks/lein-how-to-ns "0.2.12"]` to the `:plugins` vector
of your project.clj or `:user` profile.

To lint the ns forms, printing diffs wherever there are problems:
```
lein how-to-ns check
```

To fix the ns forms that don't pass the linter:
```
lein how-to-ns fix
```

### Clojure Tools

The official Clojure CLI supports installation of thirdparty [tools][].
To install cljfmt as a tool, run:

```bash
clojure -Ttools install com.gfredericks/how-to-ns '{:git/url "https://github.com/gfredericks/how-to-ns.git" :git/tag "how-to-ns-0.2.12"}' :as how-to-ns
```

To use the tool to check for formatting errors in your project, run:

```bash
clj -Thow-to-ns check
;; or with options
clj -Thow-to-ns check '{:paths ["src" "test"] :require-docstring? false}'
```

And to fix those errors:

```bash
clj -Thow-to-ns fix
;; or with options
clj -Thow-to-ns fix '{:paths ["src" "test"] :require-docstring? false}'
```

**NOTE!** The default value for `:paths` is `["src" "test"]`.
Other defaults are explained in the Customization section below.

[tools]: https://clojure.org/reference/deps_and_cli#tool_install



### Library

    Maven coordinates: `[com.gfredericks/how-to-ns "0.2.12"]`

``` clojure
(require '[com.gfredericks.how-to-ns :as how-to-ns])

;; see below for description of opts
(how-to-ns/good-ns-str?               ns-str opts)
(how-to-ns/format-ns-str              ns-str opts)
(how-to-ns/starts-with-good-ns-str? file-str opts)
(how-to-ns/format-initial-ns-str    file-str opts)
```

## Disclaimer

As of version `0.2.6`, `how-to-ns` has preliminary support for reader
conditionals, but the exact behavior is subject to change.

## Customization

Either via the `opts` param in the library or a `:how-to-ns` entry in
your `project.clj` or `:user` profile for the lein plugin, the
following options are available, shown here with their default values:

``` clojure
{:require-docstring?               true
 :sort-clauses?                    true
 :allow-refer-all?                 false
 :allow-rename?                    false
 :allow-extra-clauses?             false
 :align-clauses?                   false
 :import-square-brackets?          false
 :place-string-requires-at-bottom? false
 ;; if `true`, doesn't place a newline after `(:require`:
 :traditional-newline-style?       false
 ;; if `true` make `(:require\n  <add-one-more-space-symbol-here>[clojure.string :as ...`
 ;; This option only has effect with `:traditional-newline-style?` set to false
 :cursive-indentation?             false}
```

## Things it doesn't do until somebody makes it do them

- preserve comments or any other irregular whitespace
- rich support for .cljc
  - There's some basic/preliminary support for reader conditionals now, although it's not particularly prettified or canonical.

## License

Copyright © 2016-2021 Gary Fredericks

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
