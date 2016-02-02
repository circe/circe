# circe

[![Build status](https://img.shields.io/travis/travisbrown/circe/master.svg)](https://travis-ci.org/travisbrown/circe)
[![Coverage status](https://img.shields.io/codecov/c/github/travisbrown/circe/master.svg)](https://codecov.io/github/travisbrown/circe)
[![Gitter](https://img.shields.io/badge/gitter-join%20chat-green.svg)](https://gitter.im/travisbrown/circe)
[![Maven Central](https://img.shields.io/maven-central/v/io.circe/circe-core_2.11.svg)](https://maven-badges.herokuapp.com/maven-central/io.circe/circe-core_2.11)

circe is a JSON library for Scala (and [Scala.js][scala-js]). The rest of this page tries to give
some justification for its existence. There are also [API docs][api].

circe's working title was jfc, which stood for "JSON for [cats][cats]". The name was changed for [a
number of reasons](https://github.com/travisbrown/circe/issues/11).

## Table of contents

1. [Quick start](#quick-start)
2. [Why?](#why)
  1. [Dependencies and modularity](#dependencies-and-modularity)
  2. [Parsing](#parsing)
  3. [Lenses](#lenses)
  4. [Codec derivation](#codec-derivation)
  5. [Aliases](#aliases)
  6. [Documentation](#documentation)
  7. [Testing](#testing)
  8. [Performance](#performance)
3. [Usage](#usage)
  1. [Encoding and decoding](#encoding-and-decoding)
  2. [Transforming JSON](#transforming-json)
4. [Contributors and participation](#contributors-and-participation)
5. [Warnings and known issues](#warnings-and-known-issues)
6. [License](#license)

## Quick start

circe is published to [Maven Central][maven-central] and cross-built for Scala 2.10 and 2.11, so
you can just add the following to your build:

```scala
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % "0.2.1",
  "io.circe" %% "circe-generic" % "0.2.1",
  "io.circe" %% "circe-parse" % "0.2.1"
)
```

If you are using circe's generic derivation with Scala 2.10, you'll also need to include the [Macro
Paradise][paradise] compiler plugin in your build:

```scala
addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full
)
```

Then type `sbt console` to start a REPL and then paste the following (this will also work from the
root directory of this repository):

```scala
scala> import io.circe._, io.circe.generic.auto._, io.circe.parse._, io.circe.syntax._
import io.circe._
import io.circe.generic.auto._
import io.circe.parse._
import io.circe.syntax._

scala> sealed trait Foo
defined trait Foo

scala> case class Bar(xs: List[String]) extends Foo
defined class Bar

scala> case class Qux(i: Int, d: Option[Double]) extends Foo
defined class Qux

scala> val foo: Foo = Qux(13, Some(14.0))
foo: Foo = Qux(13,Some(14.0))

scala> foo.asJson.noSpaces
res0: String = {"Qux":{"d":14.0,"i":13}}

scala> decode[Foo](foo.asJson.spaces4)
res1: cats.data.Xor[io.circe.Error,Foo] = Right(Qux(13,Some(14.0)))
```

No boilerplate, no runtime reflection.

## Why?

[Argonaut][argonaut] is a great library. It's by far the best JSON library for Scala, and the best
JSON library on the JVM. If you're doing anything with JSON in Scala, you should be using Argonaut.

circe is a fork of Argonaut with a few important differences.

### Dependencies and modularity

circe depends on [cats][cats] instead of [Scalaz][scalaz], and the `core` project has only two
dependencies: cats-core and [export-hook][export-hook] (a lightweight mechanism for cleaner generic
type class instance derivation).

Other subprojects bring in dependencies on [Jawn][jawn] (for parsing in the [`jawn`][circe-jawn]
subproject), [Shapeless][shapeless] (for automatic codec derivation in [`generic`][circe-generic]),
and [Twitter Util][util] (for tools for asynchronous parsing in `async`), but it would be possible
to replace the functionality provided by these subprojects with alternative implementations that use
other libraries.

### Parsing

circe doesn't include a JSON parser in the `core` project, which is focused on the JSON AST, zippers,
and codecs. The [`jawn`][circe-jawn] subproject provides support for parsing JSON via a [Jawn][jawn]
facade. Jawn is fast, it offers asynchronous parsing, and best of all it lets us drop a lot of the
fussiest code in Argonaut. The [`jackson`][circe-jackson] subproject supports using
[Jackson][jackson] for both parsing and printing.

circe also provides a [`parse`][circe-parse] subproject that provides parsing support for Scala.js,
with JVM parsing provided by `io.circe.jawn` and JavaScript parsing from `scalajs.js.JSON`.

### Lenses

circe doesn't use or provide lenses in the `core` project (or at all, for now). This is related to
the first point above, since [Monocle][monocle] has a Scalaz dependency, but we also feel that it
simplifies the API. We are likely to add [an experimental `optics` subproject][optics-pr] in 0.3.0
that will provide Monocle lenses (note that this will require your project to depend on both Scalaz
and cats).

### Codec derivation

circe does not use macros or provide any kind of automatic derivation in the `core` project. Instead
of Argonaut's limited macro-based derivation (which  does not support sealed trait hierarchies, for
example), circe includes a subproject (`generic`) that provides generic codec derivation using
[Shapeless][shapeless].

[This subproject][circe-generic] is currently a simplified port of
[argonaut-shapeless][argonaut-shapeless] that provides fully automatic derivation of instances for
case classes and sealed trait hierarchies. It also includes derivation of "incomplete" case class
instances (see my recent [blog post][incompletes] for details).

### Aliases

circe aims to simplify Argonaut's API by removing all operator aliases. This is largely a matter of
personal taste, and may change in the future.

### Documentation

The Argonaut documentation is good, but it could be better: to take just one example, it can be hard
to tell at a glance why there are three different `Cursor`, `HCursor`, and `ACursor` types. In this
particular case, circe introduces an abstraction over cursors that makes the relationship clearer and
allows these three types to [share API documentation][generic-cursor].

### Testing

I'd like to provide more complete test coverage (in part via [Discipline][discipline]), but it's
early days for this.

### Performance

circe aims to be more focused on performance. I'm still experimenting with the right balance, but I'm
open to using mutability, inheritance, and all kinds of other horrible things under the hood if they
make circe faster (the public API does not and will never expose any of this, though).

[My initial benchmarks][benchmarks] suggest this is at least kind of working (higher numbers are
better):

```
Benchmark                       Mode  Cnt       Score       Error  Units

DecodingBenchmark.decodeFoosA  thrpt   80    1319.560 ±    41.424  ops/s
DecodingBenchmark.decodeFoosC  thrpt   80    2468.946 ±    24.422  ops/s
DecodingBenchmark.decodeFoosP  thrpt   80    1588.493 ±     8.353  ops/s

DecodingBenchmark.decodeIntsA  thrpt   80    7520.004 ±   121.529  ops/s
DecodingBenchmark.decodeIntsC  thrpt   80   13123.287 ±   184.418  ops/s
DecodingBenchmark.decodeIntsP  thrpt   80   11698.379 ±    19.362  ops/s

EncodingBenchmark.encodeFoosA  thrpt   80    5998.060 ±    34.348  ops/s
EncodingBenchmark.encodeFoosC  thrpt   80    6643.592 ±    33.206  ops/s
EncodingBenchmark.encodeFoosP  thrpt   80    2302.302 ±     5.027  ops/s

EncodingBenchmark.encodeIntsA  thrpt   80   59777.605 ±   109.100  ops/s
EncodingBenchmark.encodeIntsC  thrpt   80   95857.463 ±   198.219  ops/s
EncodingBenchmark.encodeIntsP  thrpt   80   57254.911 ±   211.518  ops/s

ParsingBenchmark.parseFoosA    thrpt   80    2437.300 ±   143.402  ops/s
ParsingBenchmark.parseFoosC    thrpt   80    3254.879 ±    25.084  ops/s
ParsingBenchmark.parseFoosCJ   thrpt   80    2760.303 ±     5.919  ops/s

ParsingBenchmark.parseIntsA    thrpt   80   11397.083 ±    73.684  ops/s
ParsingBenchmark.parseIntsC    thrpt   80   34432.010 ±    74.800  ops/s
ParsingBenchmark.parseIntsP    thrpt   80   14687.614 ±    86.518  ops/s

PrintingBenchmark.printFoosA   thrpt   80    2797.349 ±    19.894  ops/s
PrintingBenchmark.printFoosC   thrpt   80    3638.720 ±    11.466  ops/s
PrintingBenchmark.printFoosP   thrpt   80    7310.970 ±    28.968  ops/s

PrintingBenchmark.printIntsA   thrpt   80   15534.731 ±   142.490  ops/s
PrintingBenchmark.printIntsC   thrpt   80   22597.027 ±   134.339  ops/s
PrintingBenchmark.printIntsP   thrpt   80   74502.461 ±   519.386  ops/s
```

And allocation rates (lower is better):

```
Benchmark                                         Mode  Cnt        Score        Error   Units

DecodingBenchmark.decodeFoosA:gc.alloc.rate.norm thrpt   20  3732265.409 ±  35992.955    B/op
DecodingBenchmark.decodeFoosC:gc.alloc.rate.norm thrpt   20  1692832.673 ±      1.310    B/op
DecodingBenchmark.decodeFoosP:gc.alloc.rate.norm thrpt   20  2126657.073 ±      2.084    B/op

DecodingBenchmark.decodeIntsA:gc.alloc.rate.norm thrpt   20   623401.104 ±     22.410    B/op
DecodingBenchmark.decodeIntsC:gc.alloc.rate.norm thrpt   20   326512.124 ±      0.240    B/op
DecodingBenchmark.decodeIntsP:gc.alloc.rate.norm thrpt   20   369120.144 ±      0.279    B/op

EncodingBenchmark.encodeFoosA:gc.alloc.rate.norm thrpt   20   521917.560 ±      4.615    B/op
EncodingBenchmark.encodeFoosC:gc.alloc.rate.norm thrpt   20   414910.061 ±   3561.697    B/op
EncodingBenchmark.encodeFoosP:gc.alloc.rate.norm thrpt   20  1338208.745 ±      1.445    B/op

EncodingBenchmark.encodeIntsA:gc.alloc.rate.norm thrpt   20    80152.030 ±      0.058    B/op
EncodingBenchmark.encodeIntsC:gc.alloc.rate.norm thrpt   20    48360.018 ±      0.036    B/op
EncodingBenchmark.encodeIntsP:gc.alloc.rate.norm thrpt   20    71352.030 ±      0.057    B/op

ParsingBenchmark.parseFoosA:gc.alloc.rate.norm   thrpt   20  1450360.708 ±      1.402    B/op
ParsingBenchmark.parseFoosC:gc.alloc.rate.norm   thrpt   20   731299.376 ±      7.625    B/op
ParsingBenchmark.parseFoosP:gc.alloc.rate.norm   thrpt   20   982920.788 ±      1.530    B/op

ParsingBenchmark.parseIntsA:gc.alloc.rate.norm   thrpt   20   310280.146 ±      0.281    B/op
ParsingBenchmark.parseIntsC:gc.alloc.rate.norm   thrpt   20   105232.049 ±      0.095    B/op
ParsingBenchmark.parseIntsP:gc.alloc.rate.norm   thrpt   20   200464.116 ±      0.225    B/op

PrintingBenchmark.printFoosA:gc.alloc.rate.norm  thrpt   20   608120.589 ±      1.141    B/op
PrintingBenchmark.printFoosC:gc.alloc.rate.norm  thrpt   20   423696.465 ±      0.899    B/op
PrintingBenchmark.printFoosP:gc.alloc.rate.norm  thrpt   20   348896.235 ±      0.449    B/op

PrintingBenchmark.printIntsA:gc.alloc.rate.norm  thrpt   20   239712.111 ±      0.214    B/op
PrintingBenchmark.printIntsC:gc.alloc.rate.norm  thrpt   20    95408.075 ±      0.145    B/op
PrintingBenchmark.printIntsP:gc.alloc.rate.norm  thrpt   20    24080.022 ±      0.043    B/op
```

The `Foos` benchmarks work with a map containing case class values, and the `Ints` ones are an array
of integers. `C` suffixes indicate circe's throughput, `A` is for Argonaut, and `P` is for
[play-json][play-json].

## Usage

This section needs a lot of expanding.

### Encoding and decoding

circe uses `Encoder` and `Decoder` type classes for encoding and decoding. An `Encoder[A]` instance
provides a function that will convert any `A` to a `JSON`, and a `Decoder[A]` takes a `Json` value
to either an exception or an `A`. circe provides implicit instances of these type classes for many
types from the Scala standard library, including `Int`, `String`, and [others][encoder]. It also
provides instances for `List[A]`, `Option[A]`, and other generic types, but only if `A` has an
`Encoder` instance.

### Transforming JSON

Suppose we have the following JSON document:

```scala
import io.circe._, io.circe.generic.auto._, io.circe.jawn._, io.circe.syntax._
import cats.data.Xor

val json: String = """
  {
    "id": "c730433b-082c-4984-9d66-855c243266f0",
    "name": "Foo",
    "counts": [1, 2, 3],
    "values": {
      "bar": true,
      "baz": 100.001,
      "qux": ["a", "b"]
    }
  }
"""

val doc: Json = parse(json).getOrElse(Json.empty)
```

In order to transform this document we need to create an `HCursor` with the focus at the document's
root:

```scala
val cursor: HCursor = doc.hcursor
```

We can then use [various operations][generic-cursor] to move the focus of the cursor around the
document and to "modify" the current focus:

```scala
val reversedNameCursor: ACursor =
  cursor.downField("name").withFocus(_.mapString(_.reverse))
```

We can then return to the root of the document and return its value with `top`:

```scala
val reversedName: Option[Json] = reversedNameCursor.top
```

The result will contain the original document with the `"name"` field reversed.

## Contributors and participation

circe is a fork of Argonaut, and if you find it at all useful, you should thank
[Mark Hibberd][markhibberd], [Tony Morris][tonymorris], [Kenji Yoshida][xuwei-k], and the rest of
the [Argonaut contributors][argonaut-contributors].

circe is currently maintained by [Travis Brown][travisbrown], [Alexandre Archambault][archambault],
and [Vladimir Kostyukov][vkostyukov]. After the 0.3.0 release, all pull requests will require two
sign-offs by a maintainer to be merged.

The circe project supports the [Typelevel][typelevel] [code of conduct][code-of-conduct] and wants
all of its channels (Gitter, GitHub, etc.) to be welcoming environments for everyone.

Please see the [contributors' guide](CONTRIBUTING.md) for details on how to submit a pull request.

## Warnings and known issues

1. Please note that generic derivation will not work on Scala 2.10 unless you've added the [Macro
   Paradise][paradise] plugin to your build. See the [quick start section](#quick-start) above for
   details.
2. In the 0.3.0 snapshot, the `io.circe.generic` package depends on the Shapeless 2.3.0 snapshot,
   which means that in principle it may stop working at any time. 0.3.0 will not be released until
   Shapeless 2.3.0 is available, and of course we will never publish a stable version with snapshot
   dependencies.
3. The `refined` subproject (available only in the 0.3.0 snapshot) depends on refined 0.3.1, which
   depends on Shapeless 2.2.5, which means that if you use it, you'll have to cross your fingers and
   hope that you don't run into binary compatibility issues. This will be resolved before the 0.3.0
   release, and the risk should be low in the meantime (because of how refined uses Shapeless), but
   there is a chance you will run into problems.
4. For large or deeply-nested case classes and sealed trait hierarchies, the generic derivation
   provided by the `generic` subproject may stack overflow during compilation, which will result in
   the derived encoders or decoders simply not being found. Increasing the stack size available to
   the compiler (e.g. with `sbt -J-Xss64m` if you're using SBT) will help in many cases, but we have
   at least [one report][very-large-adt] of a case where it doesn't.
5. More generally, the generic derivation provided by the `generic` subproject works for a wide
   range of test cases, and is likely to _just work_ for you, but it relies on macros (provided by
   Shapeless) that rely on compiler functionality that is not always perfectly robust
   ("[SI-7046][si-7046] is like [playing roulette][si-7046-roulette]"), and if you're running into
   problems, it's likely that they're not your fault. Please file an issue here or ask a question on
   the [Gitter channel][gitter], and we'll do our best to figure out whether the problem is
   something we can fix.

## License

circe is licensed under the **[Apache License, Version 2.0][apache]** (the
"License"); you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[apache]: http://www.apache.org/licenses/LICENSE-2.0
[api]: https://travisbrown.github.io/circe/api/#io.circe.package
[archambault]: https://twitter.com/alxarchambault
[argonaut]: http://argonaut.io/
[argonaut-contributors]: https://github.com/argonaut-io/argonaut/graphs/contributors
[argonaut-shapeless]: https://github.com/alexarchambault/argonaut-shapeless
[benchmarks]: https://github.com/travisbrown/circe/blob/topic/plugins/benchmark/src/main/scala/io/circe/benchmark/Benchmark.scala
[cats]: https://github.com/typelevel/cats
[circe-generic]: https://travisbrown.github.io/circe/api/#io.circe.generic.auto$
[circe-jackson]: https://travisbrown.github.io/circe/api/#io.circe.jackson.package
[circe-jawn]: https://travisbrown.github.io/circe/api/#io.circe.jawn.package
[circe-parse]: https://travisbrown.github.io/circe/api/#io.circe.parse.package
[code-of-conduct]: http://typelevel.org/conduct.html
[discipline]: https://github.com/typelevel/discipline
[encoder]: https://travisbrown.github.io/circe/api/#io.circe.Encoder$
[export-hook]: https://github.com/milessabin/export-hook
[finch]: https://github.com/finagle/finch
[generic-cursor]: https://travisbrown.github.io/circe/api/#io.circe.GenericCursor
[gitter]: https://gitter.im/travisbrown/circe
[incompletes]: https://meta.plasm.us/posts/2015/06/21/deriving-incomplete-type-class-instances/
[jackson]: https://github.com/FasterXML/jackson
[jawn]: https://github.com/non/jawn
[markhibberd]: https://github.com/markhibberd
[maven-central]: http://search.maven.org/
[monocle]: https://github.com/julien-truffaut/Monocle
[optics-pr]: https://github.com/travisbrown/circe/pull/78
[paradise]: http://docs.scala-lang.org/overviews/macros/paradise.html
[play-json]: https://www.playframework.com/documentation/2.4.x/ScalaJson
[scala-js]: http://www.scala-js.org/
[scalaz]: https://github.com/scalaz/scalaz
[shapeless]: https://github.com/milessabin/shapeless
[si-7046]: https://issues.scala-lang.org/browse/SI-7046
[si-7046-roulette]: https://twitter.com/li_haoyi/status/637281580847878145
[spool]: https://twitter.github.io/util/docs/index.html#com.twitter.concurrent.Spool
[tonymorris]: https://github.com/tonymorris
[travisbrown]: https://twitter.com/travisbrown
[typelevel]: http://typelevel.org/
[util]: https://github.com/twitter/util
[very-large-adt]: http://stackoverflow.com/questions/33318802/scala-parse-json-of-more-than-22-elements-into-case-class/33319168?noredirect=1#comment55069438_33319168
[vkostyukov]: https://twitter.com/vkostyukov
[xuwei-k]: https://github.com/xuwei-k
