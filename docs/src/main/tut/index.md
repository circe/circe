---
layout: home
title:  "Home"
section: "home"
---

[![Build status](https://img.shields.io/travis/circe/circe/master.svg)](https://travis-ci.org/circe/circe)
[![Coverage status](https://img.shields.io/codecov/c/github/circe/circe/master.svg)](https://codecov.io/github/circe/circe)
[![Gitter](https://img.shields.io/badge/gitter-join%20chat-green.svg)](https://gitter.im/circe/circe)
[![Maven Central](https://img.shields.io/maven-central/v/io.circe/circe-core_2.13.svg)](https://maven-badges.herokuapp.com/maven-central/io.circe/circe-core_2.13)

circe (pronounced SUR-see, or KEER-kee in classical Greek, or CHEER-chay in Ecclesiastical Latin) is
a JSON library for Scala (and [Scala.js][scala-js]).

circe's working title was jfc, which stood for "JSON for [cats][cats]". The name was changed for
[a number of reasons](https://github.com/circe/circe/issues/11).

<a name="quick-start"></a>

{% include_relative quickstart.md %}

## Why?

[Argonaut][argonaut] is a great library. It's by far the best JSON library for Scala, and the best
JSON library on the JVM. If you're doing anything with JSON in Scala, you should be using Argonaut.

circe is a fork of Argonaut with a few important differences.

### Dependencies and modularity

circe depends on [cats][cats] instead of [Scalaz][scalaz], and the `core` project has only one
dependency (cats-core).

Other subprojects bring in dependencies on [Jawn][jawn] (for parsing in the [`jawn`][circe-jawn]
subproject), [Shapeless][shapeless] (for automatic codec derivation in [`generic`][circe-generic]),
but it would be possible to replace the functionality provided by these subprojects with alternative
implementations that use other libraries.

### Parsing

circe doesn't include a JSON parser in the `core` project, which is focused on the JSON AST, zippers,
and codecs. The [`jawn`][circe-jawn] subproject provides support for parsing JSON via a [Jawn][jawn]
facade. Jawn is fast, it offers asynchronous parsing, and best of all it lets us drop a lot of the
fussiest code in Argonaut. The [circe-jackson][circe-jackson] project supports using
[Jackson][jackson] for both parsing and printing.

circe also provides a [`parser`][circe-parser] subproject that provides parsing support for Scala.js,
with JVM parsing provided by `io.circe.jawn` and JavaScript parsing from `scalajs.js.JSON`.

See the [Parsing page](parsing.html) for more details.

### Lenses

circe doesn't use or provide lenses in the `core` project. This is related to the first point above,
since [Monocle][monocle] has a Scalaz dependency, but we also feel that it simplifies the API. The
0.3.0 release added [an experimental `optics` subproject][optics-pr] that provides Monocle lenses.

See the [Optics page](optics.html) for more details.

### Codec derivation

circe does not use macros or provide any kind of automatic derivation in the `core` project. Instead
of Argonaut's limited macro-based derivation (which does not support sealed trait hierarchies, for
example), circe includes a subproject (`generic`) that provides generic codec derivation using
[Shapeless][shapeless].

[This subproject][circe-generic] is currently a simplified port of
[argonaut-shapeless][argonaut-shapeless] that provides fully automatic derivation of instances for
case classes and sealed trait hierarchies. It also includes derivation of "incomplete" case class
instances (see my recent [blog post][incompletes] for details). Note that if you use
`-Ypartial-unification` and `auto`, incomplete decoders will not work (see
[#724](https://github.com/circe/circe/pull/724)).

See the [Encoding and Decoding page](codec.html) for more details.

### Aliases

circe aims to simplify Argonaut's API by removing all operator aliases. This is largely a matter of
personal taste, and may change in the future.

### Testing

I'd like to provide more complete test coverage (in part via [Discipline][discipline]), but it's
early days for this.

### Performance

circe is developed with a focus on performance. See the [Performance](performance.html) page for
details.

## License

circe is licensed under the **[Apache License, Version 2.0][apache]** (the
"License"); you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

{% include references.md %}
