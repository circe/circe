# circe

[![Build status](https://img.shields.io/travis/travisbrown/circe/master.svg)](https://travis-ci.org/travisbrown/circe)
[![Coverage status](https://img.shields.io/codecov/c/github/travisbrown/circe/master.svg)](https://codecov.io/github/travisbrown/circe)
[![Gitter](https://img.shields.io/badge/gitter-join%20chat-green.svg)](https://gitter.im/travisbrown/circe)
[![Maven Central](https://img.shields.io/maven-central/v/io.circe/circe-core_2.11.svg)](https://maven-badges.herokuapp.com/maven-central/io.circe/circe-core_2.11)

circe is a JSON library for Scala. The rest of this page tries to give some justification for its
existence. There are also [API docs][api].

circe's working title was jfc, which stood for "JSON for [cats][cats]". The name was changed for [a
number of reasons](https://github.com/travisbrown/circe/issues/11).

## Quick start

circe is published to [Maven Central][maven-central] and cross-built for Scala 2.10 and 2.11, so
you can just add the following to your build:

```scala
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % "0.1.1",
  "io.circe" %% "circe-generic" % "0.1.1",
  "io.circe" %% "circe-jawn" % "0.1.1"
)
```

Then type `sbt console` to start a REPL and then paste the following (this will also work from the
root directory of this repository):

```scala
scala> import io.circe._, io.circe.generic.auto._, io.circe.jawn._, io.circe.syntax._
import io.circe._
import io.circe.jawn._
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
fussiest code in Argonaut.

### Lenses

circe doesn't use or provide lenses in the `core` project (or at all, for now). This is related to
the first point above, since [Monocle][monocle] has a Scalaz dependency, but we also feel that it
simplifies the API. We'd consider adding lenses in a subproject if Monocle (or something similar)
gets ported to cats.

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
Benchmark                       Mode  Cnt      Score     Error  Units

DecodingBenchmark.decodeFoosA  thrpt   40   1219.959 ±  28.183  ops/s
DecodingBenchmark.decodeFoosC  thrpt   40   1397.195 ±  15.402  ops/s

DecodingBenchmark.decodeIntsA  thrpt   40   7538.238 ± 158.823  ops/s
DecodingBenchmark.decodeIntsC  thrpt   40   7775.231 ± 121.456  ops/s

EncodingBenchmark.encodeFoosA  thrpt   40   6186.539 ±  17.022  ops/s
EncodingBenchmark.encodeFoosC  thrpt   40   6552.408 ± 127.446  ops/s

EncodingBenchmark.encodeIntsA  thrpt   40  47262.384 ± 118.791  ops/s
EncodingBenchmark.encodeIntsC  thrpt   40  92476.811 ± 367.129  ops/s

ParsingBenchmark.parseFoosA    thrpt   40   2141.975 ±  13.181  ops/s
ParsingBenchmark.parseFoosC    thrpt   40   3102.288 ±  19.494  ops/s

ParsingBenchmark.parseIntsA    thrpt   40  10820.259 ±  38.952  ops/s
ParsingBenchmark.parseIntsC    thrpt   40  33479.046 ±  94.416  ops/s

PrintingBenchmark.printFoosA   thrpt   40   2873.823 ±  32.944  ops/s
PrintingBenchmark.printFoosC   thrpt   40   3656.773 ±   9.829  ops/s

PrintingBenchmark.printIntsA   thrpt   40  18641.201 ± 165.950  ops/s
PrintingBenchmark.printIntsC   thrpt   40  22031.479 ±  80.269  ops/s
```

And allocation rates (lower is better):

```
Benchmark                                        Cnt        Score        Error   Units

DecodingBenchmark.decodeFoosA:gc.alloc.rate.norm  20  3129281.175 ±      2.273    B/op
DecodingBenchmark.decodeFoosC:gc.alloc.rate.norm  20  2745809.199 ±  57210.951    B/op

DecodingBenchmark.decodeIntsA:gc.alloc.rate.norm  20   599399.891 ±  21404.111    B/op
DecodingBenchmark.decodeIntsC:gc.alloc.rate.norm  20   528852.711 ±   5356.383    B/op

EncodingBenchmark.encodeFoosA:gc.alloc.rate.norm  20   526700.440 ±   1425.759    B/op
EncodingBenchmark.encodeFoosC:gc.alloc.rate.norm  20   429129.647 ±   1426.476    B/op

EncodingBenchmark.encodeIntsA:gc.alloc.rate.norm  20    88144.033 ±   7134.445    B/op
EncodingBenchmark.encodeIntsC:gc.alloc.rate.norm  20    48360.018 ±      0.035    B/op

ParsingBenchmark.parseFoosA:gc.alloc.rate.norm    20  1464616.780 ±   2138.191    B/op
ParsingBenchmark.parseFoosC:gc.alloc.rate.norm    20   735249.500 ±      2.852    B/op

ParsingBenchmark.parseIntsA:gc.alloc.rate.norm    20   326296.548 ±      1.540    B/op
ParsingBenchmark.parseIntsC:gc.alloc.rate.norm    20   105232.050 ±      0.096    B/op

PrintingBenchmark.printFoosA:gc.alloc.rate.norm   20   595216.561 ±   9978.237    B/op
PrintingBenchmark.printFoosC:gc.alloc.rate.norm   20   386712.468 ±   4190.873    B/op

PrintingBenchmark.printIntsA:gc.alloc.rate.norm   20   119592.091 ±      0.175    B/op
PrintingBenchmark.printIntsC:gc.alloc.rate.norm   20    95408.076 ±      0.147    B/op
```

The `Foos` benchmarks work with a map containing case class values, and the `Ints` ones are an array
of integers. `C` suffixes indicate circe's throughput and `A` is for Argonaut.

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
[cats]: https://github.com/non/cats
[code-of-conduct]: http://typelevel.org/conduct.html
[discipline]: https://github.com/typelevel/discipline
[encoder]: https://travisbrown.github.io/circe/api/#io.circe.Encoder$
[export-hook]: https://github.com/milessabin/export-hook
[finch]: https://github.com/finagle/finch
[generic-cursor]: https://travisbrown.github.io/circe/api/#io.circe.GenericCursor
[incompletes]: https://meta.plasm.us/posts/2015/06/21/deriving-incomplete-type-class-instances/
[jawn]: https://github.com/non/jawn
[circe-generic]: https://travisbrown.github.io/circe/api/#io.circe.generic.auto$
[circe-jawn]: https://travisbrown.github.io/circe/api/#io.circe.jawn.package
[markhibberd]: https://github.com/markhibberd
[maven-central]: http://search.maven.org/
[monocle]: https://github.com/julien-truffaut/Monocle
[scalaz]: https://github.com/scalaz/scalaz
[shapeless]: https://github.com/milessabin/shapeless
[spool]: https://twitter.github.io/util/docs/index.html#com.twitter.concurrent.Spool
[tonymorris]: https://github.com/tonymorris
[travisbrown]: https://twitter.com/travisbrown
[typelevel]: http://typelevel.org/
[util]: https://github.com/twitter/util
[vkostyukov]: https://twitter.com/vkostyukov
[xuwei-k]: https://github.com/xuwei-k
