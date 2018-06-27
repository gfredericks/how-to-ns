# how-to-ns changelog

## 0.2.1 2018-06-26

Split into library (`com.gfredericks.how-to-ns`) and lein plugin
(`com.gfredericks.lein-how-to-ns`).

Added four new functions to the `com.gfredericks.how-to-ns` namespace,
and a `com.gfredericks.how-to-ns.main` namespace which has some path
searching functionality used by the plugin, and can have a proper CLI
in the future.

## 0.1.9 2018-05-31

Move the implementation to the `com.gfredericks.how-to-ns` namespace,
beginning to separate the core functionality from the leiningen plugin
([#7](https://github.com/gfredericks/how-to-ns/pull/7)).

## 0.1.8 2017-12-05

[Adds opt-in support for :rename](https://github.com/gfredericks/how-to-ns/issues/5).

## 0.1.7 2017-10-21

[Adds support for :require-macros](https://github.com/gfredericks/how-to-ns/issues/4#issuecomment-338344766).

## 0.1.6 2017-05-10

Clarified docstring.

## 0.1.5 2017-02-11

Bugfix: Don't exit the jvm on successful checks.

## 0.1.4 2017-02-06

Add support for `:gen-class` and ns metadata.

## 0.1.3 2016-10-20

Fix `lein help how-to-ns` bug.

## 0.1.2 2016-10-20

Improve usage/docstrings.

## 0.1.1 2016-10-20

Change default docstring.

## 0.1.1 2016-10-20

Initial release
