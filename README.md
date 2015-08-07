# jfc

[![Build status](https://img.shields.io/travis/travisbrown/jfc/master.svg)](https://travis-ci.org/travisbrown/jfc)
[![Coverage status](https://img.shields.io/codecov/c/github/travisbrown/jfc/master.svg)](https://codecov.io/github/travisbrown/jfc)
[![Gitter](https://img.shields.io/badge/gitter-join%20chat-green.svg)](https://gitter.im/travisbrown/jfc)


jfc is a JSON library for Scala. The rest of this page tries to give some justification for its
existence. There are also [API docs][api].

The name stands for "JSON for [cats][cats]" and is a working title that
[is being changed](https://github.com/travisbrown/jfc/issues/11).

## Showing off

Type `sbt console` to start a REPL in the root project, and then paste the following:

```scala
scala> import io.jfc._, io.jfc.auto._, io.jfc.jawn._, io.jfc.syntax._
import io.jfc._
import io.jfc.auto._
import io.jfc.jawn._
import io.jfc.syntax._

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
res1: cats.data.Xor[io.jfc.Error,Foo] = Right(Qux(13,Some(14.0)))
```

No boilerplate, no runtime reflection.

## Why?

[Argonaut][argonaut] is a great library. It's by far the best JSON library for Scala, and the best
JSON library on the JVM. If you're doing anything with JSON in Scala, you should be using Argonaut.

jfc is a fork of Argonaut with a few important differences.

### Dependencies and modularity

jfc depends on [cats][cats] instead of [Scalaz][scalaz], and cats is the only dependency of the
`core` project.

Other subprojects bring in dependencies on [Jawn][jawn] (for parsing in the [`jawn`][jfc-jawn]
subproject), [Shapeless][shapeless] (for automatic codec derivation in [`auto`][jfc-auto]), and
[Twitter Util][util] (for tools for asynchronous parsing in `async`), but it would be possible to
replace the functionality provided by these subprojects with alternative implementations that use
other libraries.

### Parsing

jfc doesn't include a JSON parser in the `core` project, which is focused on the JSON AST, zippers,
and codecs. The [`jawn`][jfc-jawn] subproject provides support for parsing JSON via a [Jawn][jawn]
facade. Jawn is fast, it offers asynchronous parsing, and best of all it lets us drop a lot of the
fussiest code in Argonaut.

### Lenses

jfc doesn't use or provide lenses in the `core` project (or at all, for now). This is related to
the first point above, since [Monocle][monocle] has a Scalaz dependency, but we also feel that it
simplifies the API. We'd consider adding lenses in a subproject if Monocle (or something similar)
gets ported to cats.

### Codec derivation

jfc does not use macros or provide any kind of automatic derivation in the `core` project. Instead
of Argonaut's limited macro-based derivation (which  does not support sealed trait hierarchies, for
example), jfc includes a subproject (`auto`) that provides generic codec derivation using
[Shapeless][shapeless].

[This subproject][jfc-auto] is currently a simplified port of
[argonaut-shapeless][argonaut-shapeless] that provides fully-automatic derivation of instances for
tuples, case classes, and sealed trait hierarchies. It also includes derivation of "incomplete" case classes (see my recent [blog post][incompletes] for details).

We may eventually include an additional subproject with less automatic, more
customizable codec derivation.

### Aliases

jfc aims to simplify Argonaut's API by removing all operator aliases. This is largely a matter of
personal taste, and may change in the future.

### Documentation

The Argonaut documentation is good, but it could be better: to take just one example, it can be hard
to tell at a glance why there are three different `Cursor`, `HCursor`, and `ACursor` types. In this
particular case, jfc introduces an abstraction over cursors that makes the relationship clearer and
allows these three types to [share API documentation][generic-cursor].

### Testing

I'd like to provide more complete test coverage (in part via [Discipline][discipline]), but it's
early days for this.

### Performance

jfc aims to be more focused on performance. I'm still experimenting with the right balance, but I'm
open to using mutability, inheritance, and all kinds of other horrible things under the hood if they
make jfc faster (the public API does not and will never expose any of this, though).

[My initial benchmarks][benchmarks] suggest this is at least kind of working (higher numbers are
better):

```
Benchmark                       Mode  Cnt      Score     Error  Units

DecodingBenchmark.decodeFoosA  thrpt   40   1244.431 ±   2.902  ops/s
DecodingBenchmark.decodeFoosJ  thrpt   40   1452.909 ±  66.909  ops/s

DecodingBenchmark.decodeIntsA  thrpt   40   7276.600 ±  17.828  ops/s
DecodingBenchmark.decodeIntsJ  thrpt   40   7731.774 ± 151.947  ops/s

EncodingBenchmark.encodeFoosA  thrpt   40   6162.503 ±  28.440  ops/s
EncodingBenchmark.encodeFoosJ  thrpt   40   6274.457 ±  27.368  ops/s

EncodingBenchmark.encodeIntsA  thrpt   40  47304.729 ± 105.165  ops/s
EncodingBenchmark.encodeIntsJ  thrpt   40  92950.094 ± 660.824  ops/s

ParsingBenchmark.parseFoosA    thrpt   40   2130.100 ±   6.644  ops/s
ParsingBenchmark.parseFoosJ    thrpt   40   3122.276 ±  13.634  ops/s

ParsingBenchmark.parseIntsA    thrpt   40  11009.453 ±  68.750  ops/s
ParsingBenchmark.parseIntsJ    thrpt   40  33286.378 ± 115.074  ops/s

PrintingBenchmark.printFoosA   thrpt   40   2786.543 ±  32.151  ops/s
PrintingBenchmark.printFoosJ   thrpt   40   3557.423 ±  24.165  ops/s

PrintingBenchmark.printIntsA   thrpt   40  18515.516 ± 103.776  ops/s
PrintingBenchmark.printIntsJ   thrpt   40  22080.397 ±  66.809  ops/s
```

And allocation rates (lower is better):

```
Benchmark                                        Cnt        Score        Error   Units

DecodingBenchmark.decodeFoosA:gc.alloc.rate.norm  20  3371681.259 ± 215957.731    B/op
DecodingBenchmark.decodeFoosJ:gc.alloc.rate.norm  20  2822037.234 ± 173510.947    B/op

DecodingBenchmark.decodeIntsA:gc.alloc.rate.norm  20   575375.270 ±      6.607    B/op
DecodingBenchmark.decodeIntsJ:gc.alloc.rate.norm  20   522856.721 ±  10698.337    B/op

EncodingBenchmark.encodeFoosA:gc.alloc.rate.norm  20   526700.855 ±   1425.539    B/op
EncodingBenchmark.encodeFoosJ:gc.alloc.rate.norm  20   430757.361 ±     39.293    B/op

EncodingBenchmark.encodeIntsA:gc.alloc.rate.norm  20    96152.036 ±      0.069    B/op
EncodingBenchmark.encodeIntsJ:gc.alloc.rate.norm  20    48360.018 ±      0.035    B/op

ParsingBenchmark.parseFoosA:gc.alloc.rate.norm    20  1464616.768 ±   2138.193    B/op
ParsingBenchmark.parseFoosJ:gc.alloc.rate.norm    20   737673.775 ±      3.396    B/op

ParsingBenchmark.parseIntsA:gc.alloc.rate.norm    20   326296.529 ±      1.106    B/op
ParsingBenchmark.parseIntsJ:gc.alloc.rate.norm    20   105224.050 ±      7.128    B/op

PrintingBenchmark.printFoosA:gc.alloc.rate.norm   20   599472.594 ±   7611.973    B/op
PrintingBenchmark.printFoosJ:gc.alloc.rate.norm   20   386712.472 ±   4190.871    B/op

PrintingBenchmark.printIntsA:gc.alloc.rate.norm   20   179652.102 ±  53508.329    B/op
PrintingBenchmark.printIntsJ:gc.alloc.rate.norm   20    95408.077 ±      0.149    B/op
```

The `Foos` benchmarks work with a map containing case class values, and the `Ints` ones are an array
of integers. `J` suffixes indicate jfc's throughput and `A` is for Argonaut.

## Usage

This section needs a lot of expanding.

### Encoding and decoding

jfc uses `Encode` and `Decode` type classes for encoding and decoding. An `Encode[A]` instance
provides a function that will convert any `A` to a `JSON`, and a `Decode[A]` takes a `Json` value to
either an exception or an `A`. jfc provides implicit instances of these type classes for many types
from the Scala standard library, including `Int`, `String`, and [others][encode]. It also provides
instances for `List[A]`, `Option[A]`, and other generic types, but only if `A` has an `Encode`
instance.

### Transforming JSON

Suppose we have the following JSON document:

```scala
import io.jfc._, io.jfc.auto._, io.jfc.jawn._, io.jfc.syntax._
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

## Contributors

jfc is a fork of Argonaut, and if you find it at all useful, you should thank
[Mark Hibberd][markhibberd], [Tony Morris][tonymorris], [Kenji Yoshida][xuwei-k], and the rest of
the [Argonaut contributors][argonaut-contributors].

jfc is currently developed and maintained by [Travis Brown][travisbrown].

## License

jfc is licensed under the **[Apache License, Version 2.0][apache]** (the
"License"); you may not use this software except in compliance with the License.

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

[apache]: http://www.apache.org/licenses/LICENSE-2.0
[api]: https://travisbrown.github.io/circe/api/#io.jfc.package
[argonaut]: http://argonaut.io/
[argonaut-contributors]: https://github.com/argonaut-io/argonaut/graphs/contributors
[argonaut-shapeless]: https://github.com/alexarchambault/argonaut-shapeless
[benchmarks]: https://github.com/travisbrown/circe/blob/topic/plugins/benchmark/src/main/scala/io/jfc/benchmark/Benchmark.scala
[cats]: https://github.com/non/cats
[discipline]: https://github.com/typelevel/discipline
[encode]: https://travisbrown.github.io/circe/api/#io.jfc.Encode$
[finch]: https://github.com/finagle/finch
[generic-cursor]: https://travisbrown.github.io/circe/api/#io.jfc.GenericCursor
[incompletes]: https://meta.plasm.us/posts/2015/06/21/deriving-incomplete-type-class-instances/
[jawn]: https://github.com/non/jawn
[jfc-auto]: https://travisbrown.github.io/circe/api/#io.jfc.auto.package
[jfc-jawn]: https://travisbrown.github.io/circe/api/#io.jfc.jawn.package
[markhibberd]: https://github.com/markhibberd
[monocle]: https://github.com/julien-truffaut/Monocle
[scalaz]: https://github.com/scalaz/scalaz
[shapeless]: https://github.com/milessabin/shapeless
[spool]: https://twitter.github.io/util/docs/index.html#com.twitter.concurrent.Spool
[tonymorris]: https://github.com/tonymorris
[travisbrown]: https://twitter.com/travisbrown
[util]: https://github.com/twitter/util
[vkostyukov]: https://twitter.com/vkostyukov
[xuwei-k]: https://github.com/xuwei-k
