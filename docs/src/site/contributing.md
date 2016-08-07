---
layout: default
title:  "Contributing"
section: "contributing"
---

# Contributing

This project follows a standard [fork and pull][fork-and-pull] model for accepting contributions via
GitHub pull requests:

0. [Pick (or report) an issue](#pick-or-report-an-issue)
1. [Write code](#write-code)
2. [Write tests](#write-tests)
3. [Submit a pull request](#submit-a-pull-request)

## Pick or report an issue

The [_beginner-friendly_][beginner-friendly] label flags [open issues][issues] where there is
general agreement about the best way forward, and where the fix is likely to be relatively
straightforward for someone with Scala experience. The [_help wanted_][help-wanted] label is a
little more general, and indicates issues that may be much more challenging or have unresolved
questions that need additional discussion.

When you begin working on an issue, please leave a comment to notify others that the issue is taken,
and join us in the [Gitter channel][gitter] if you have any questions along the way.

We always welcome bug reports and feature requests—please don't feel like you need to have time to
contribute a fix or implementation for your issue to be appreciated.

## Write code

The [design principles document](DESIGN.md) outlines some of the practices followed in the circe
codebase. In general the public API should be purely functional, but the implementation is free to
use non-functional constructions for the sake of performance—we want correctness, of course, but
internally we're willing to have this correctness verified by tests rather than the compiler when
necessary. That said, performance-motivated optimizations should be based on evidence, not
speculation, and a functional style should be preferred whenever possible. When in doubt, get in
touch on Gitter or look around the codebase to see how things are done.

circe uses a fairly standard Scalastyle configuration to check formatting, and it may be useful to
make your IDE aware of the following rules:

* Code and comments should be formatted to a width no greater than 100 columns.
* Files should not contain trailing spaces.
* Imports should be sorted alphabetically.

When in doubt, please run `sbt scalastyle` and let us know if you have any questions.

## Write tests

circe uses three testing libraries: [Discipline][discipline], [ScalaCheck][scalacheck], and
[ScalaTest][scalatest], and organizes tests according to the following guidelines:

* In general tests live in the `tests` sub-project. This allows e.g. tests for codecs in the core
  module to be able to use the Jawn parser without circular dependencies between sub-projects.
* For experimental or stand-alone modules, it may be appropriate for tests to live in the project.
* Most test suites should extend `io.circe.tests.CirceSuite`, which provides many useful type class
  instances and other tools.
* Write tests that verify laws using Discipline whenever you can, then property-based tests using
  ScalaCheck, then behavior-driven tests.
* An assertion in regular tests should be written with `assert` and `===` (which in the context of
  `CirceSuite` is Cats's type-safe equality operator, not the operator provided by ScalaTest).
* An assertion in properties (inside `check`) should be written with `===`.

You can run `sbt +validateJVM` to run Scalastyle checking and the JVM tests. `sbt +test` will run
all tests, but requires _a lot_ of time and memory. Unless your contribution directly touches
Scala.js-related code, it's generally not necessary to run the Scala.js tests—we'll verify
compatibility before the next release.

## Submit a pull request

* Pull requests should be submitted from a separate branch (e.g. using
  `git checkout -b "username/fix-123"`).
* In general we discourage force pushing to an active pull-request branch that other people are
  commenting on or contributing to, and suggest using `git merge master` during development. Once
  development is complete, use `git rebase master` and force push to [clean up the history][squash].
* The first line of a commit message should be no more than 72 characters long (to accommodate
  formatting in various environments).
* Commit messages should general use the present tense, normal sentence capitalization, and no final
  punctuation.
* If a pull request decreases code coverage more than by 2%, please file an issue to make sure that
  tests get added.

[beginner-friendly]: https://github.com/travisbrown/circe/labels/beginner-friendly
[discipline]: https://github.com/typelevel/discipline
[gitter]: https://gitter.im/travisbrown/circe
[fork-and-pull]: https://help.github.com/articles/using-pull-requests/
[help-wanted]: https://github.com/travisbrown/circe/labels/help%20wanted
[issues]: https://github.com/travisbrown/circe/issues
[scalacheck]: https://www.scalacheck.org/
[scalatest]: http://www.scalatest.org/
[squash]: http://gitready.com/advanced/2009/02/10/squashing-commits-with-rebase.html
