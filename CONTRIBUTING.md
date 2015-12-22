Generally, circe follows a standard [fork and pull][fork-and-pull] model for contributions via
GitHub pull requests. Thus, the _contributing process_ looks as follows:

0. [Pick an issue](#pick-an-issue)
1. [Write code](#write-code)
2. [Write tests](#write-tests)
3. [Submit a PR](#submit-a-pr)

## Pick an issue

* On Github, leave a comment on the issue you picked to notify others that the issues is taken
* On [Gitter][gitter] or Github, ask any question you may have while working on the issue

## Write Code
circe follows the code style that promotes pure functional programming. When in doubt, look
around the codebase and see how it's done elsewhere.

* Code and comments should be formatted to a width no greater than 100 columns
* Files should be exempt of trailing spaces
* Imports should be sorted alphabetically

That said, the Scala style checker `sbt scalastyle` should pass on the code.

## Write Tests
circe uses the variety of testing libraries: [ScalaTest][scalatest], [ScalaCheck][scalacheck]
and [Discipline][discipline], which are used as follows:

* Most of the tests live in the `tests` sub-project
* The very base suite class `io.circe.tests.CirceSuite` brings in commons settings shared
  by most of the tests
* It's preferred to write property-based rather than a behaviour-driven tests
* An assertion in regular tests should be written with `assert(a === b)`
* An assertion in properties (inside `check`) should be written with `===`

## Submit a PR
* PR should be submitted from a separate branch (use `git checkout -b "username/fix-123"`)
* PR should generally contain only one commit (use `git commit --amend` and `git --force push` or
  [squash][squash] existing commits into one)
* PR should not decrease the code coverage more than by 1%
* PR's commit message should use present tense and be capitalized properly
  (i.e., `Fix #123: Add tests for Encoder`)

[fork-and-pull]: https://help.github.com/articles/using-pull-requests/
[scalatest]: http://www.scalatest.org/
[scalacheck]: https://www.scalacheck.org/
[discipline]: https://github.com/typelevel/discipline
[squash]: http://gitready.com/advanced/2009/02/10/squashing-commits-with-rebase.html
[gitter]: https://gitter.im/travisbrown/circe
