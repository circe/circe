Introduction and Motivation
===========
[![Build status](https://github.com/circe/circe/actions/workflows/ci.yml/badge.svg?branch=series%2F0.14.x)](https://github.com/circe/circe/actions/workflows/ci.yml)
[![Coverage status](https://img.shields.io/codecov/c/github/circe/circe/master.svg)](https://codecov.io/github/circe/circe)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.circe/circe-core_2.13/badge.svg?version=0.14.6)](https://maven-badges.herokuapp.com/maven-central/io.circe/circe-core_2.13?version=0.14.6)
[![Discord](https://img.shields.io/discord/632277896739946517.svg?label=&logo=discord&logoColor=ffffff&color=404244&labelColor=6A7EC2)](https://discord.gg/XF3CXcMzqD)

`circe` (pronounced SUR-see, or KEER-kee in classical Greek, or CHEER-chay in Ecclesiastical Latin) is
a JSON library for Scala (and [Scala.js][scala-js]).

## Why?

### Dependencies and modularity

`circe` depends on [cats][cats], and the `core` project has only one dependency (cats-core).

Other subprojects bring in dependencies on [Jawn][jawn] (for parsing in the [`jawn`][circe-jawn]
subproject), [Shapeless][shapeless] (for automatic codec derivation in [`generic`][circe-generic]),
but it would be possible to replace the functionality provided by these subprojects with alternative
implementations that use other libraries.

### Parsing

`circe` doesn't include a JSON parser in the `core` project, which is focused on the JSON AST, zippers,
and codecs. The [`jawn`][circe-jawn] subproject provides support for parsing JSON via a [Jawn][jawn]
facade. Jawn is fast, cross-platform, and offers asynchronous parsing. The [circe-jackson][circe-jackson] project supports using
[Jackson][jackson] for both parsing and printing.

`circe` also provides a [`parser`][circe-parser] subproject that provides parsing support for Scala.js,
with JVM parsing provided by `io.circe.jawn` and JavaScript parsing from `scalajs.js.JSON`.

See the [Parsing page](parsing.md) for more details.

### Lenses

`circe` doesn't use or provide lenses in the `core` project. This is related to the first point above,
since [Monocle][monocle] has a Scalaz dependency, but we also feel that it simplifies the API. The
0.3.0 release added [an experimental `optics` subproject][optics-pr] that provides Monocle lenses.

See the [Optics page](optics.md) for more details.

### Codec derivation

`circe` does not use macros or provide any kind of automatic derivation in the `core` project. Instead 
`circe` includes a subproject (`generic`) that provides generic codec derivation using
[Shapeless][shapeless].

[This subproject][circe-generic] provides fully automatic derivation of instances for
case classes and sealed trait hierarchies. It also includes derivation of "incomplete" case class
instances (see my recent [blog post][incompletes] for details). Note that if you use
`-Ypartial-unification` and `auto`, incomplete decoders will not work (see
[#724](https://github.com/circe/circe/pull/724)).

See the [Encoding and Decoding page](codecs/README.md) for more details.

### Aliases

`circe` aims to simplify its API by using no operator aliases. This is largely a matter of
personal taste, and may change in the future.

### Testing

I'd like to provide more complete test coverage (in part via [Discipline][discipline]), but it's
early days for this.

### Performance

`circe` is developed with a focus on performance. See the [Performance](performance.md) page for
details.

## License

`circe` is licensed under the **[Apache License, Version 2.0][apache]** (the
"License"); you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
