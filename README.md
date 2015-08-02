# jfc

[![Build status](https://img.shields.io/travis/travisbrown/jfc/master.svg)](https://travis-ci.org/travisbrown/jfc)
[![Coverage status](https://img.shields.io/codecov/c/github/travisbrown/jfc/master.svg)](https://codecov.io/github/travisbrown/jfc)
[![Gitter](https://img.shields.io/badge/gitter-join%20chat-green.svg)](https://gitter.im/travisbrown/jfc)


jfc is a JSON library for Scala. The name stands for "JSON for [cats][cats]". The rest of this page
tries to give some justification for its existence. There are also [API docs][api].

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

scala> foo.toJson.noSpaces
res0: String = {"Qux":{"d":14.0,"i":13}}

scala> decode[Foo](foo.toJson.spaces4)
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

[My initial benchmarks][benchmarks] suggest this is at least kind of working:

```
[info] Benchmark                       Mode  Cnt      Score      Error  Units

[info] DecodingBenchmark.decodeFoosA  thrpt   20   1160.429 ±   32.997  ops/s
[info] DecodingBenchmark.decodeFoosJ  thrpt   20   1327.090 ±  111.272  ops/s

[info] DecodingBenchmark.decodeIntsA  thrpt   20   7442.144 ±  280.426  ops/s
[info] DecodingBenchmark.decodeIntsJ  thrpt   20   7527.970 ±  294.254  ops/s

[info] EncodingBenchmark.encodeFoosA  thrpt   20   5726.027 ±  207.383  ops/s
[info] EncodingBenchmark.encodeFoosJ  thrpt   20   5984.419 ±  278.220  ops/s

[info] EncodingBenchmark.encodeIntsA  thrpt   20  42615.934 ± 3123.112  ops/s
[info] EncodingBenchmark.encodeIntsJ  thrpt   20  87456.776 ± 4621.955  ops/s

[info] ParsingBenchmark.parseFoosA    thrpt   20   2210.772 ±  218.932  ops/s
[info] ParsingBenchmark.parseFoosJ    thrpt   20   2714.505 ±  112.194  ops/s

[info] ParsingBenchmark.parseIntsA    thrpt   20  10618.503 ±  488.321  ops/s
[info] ParsingBenchmark.parseIntsJ    thrpt   20  26482.227 ±  211.512  ops/s

[info] PrintingBenchmark.printFoosA   thrpt   20   2755.286 ±   54.639  ops/s
[info] PrintingBenchmark.printFoosJ   thrpt   20   3460.610 ±   64.472  ops/s

[info] PrintingBenchmark.printIntsA   thrpt   20  15260.408 ± 1589.961  ops/s
[info] PrintingBenchmark.printIntsJ   thrpt   20  20766.068 ±  362.873  ops/s
```

The `Foos` benchmarks work with a map containing case class values, and the `Ints` ones are an array
of integers. `J` suffixes indicate jfc's throughput and `A` is for Argonaut. Higher numbers are
better.

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
[api]: https://travisbrown.github.io/jfc/api/#io.jfc.package
[argonaut]: http://argonaut.io/
[argonaut-contributors]: https://github.com/argonaut-io/argonaut/graphs/contributors
[argonaut-shapeless]: https://github.com/alexarchambault/argonaut-shapeless
[benchmarks]: https://github.com/travisbrown/jfc/blob/topic/plugins/benchmark/src/main/scala/io/jfc/benchmark/Benchmark.scala
[cats]: https://github.com/non/cats
[discipline]: https://github.com/typelevel/discipline
[finch]: https://github.com/finagle/finch
[generic-cursor]: https://travisbrown.github.io/jfc/api/#io.jfc.GenericCursor
[incompletes]: https://meta.plasm.us/posts/2015/06/21/deriving-incomplete-type-class-instances/
[jawn]: https://github.com/non/jawn
[jfc-auto]: https://travisbrown.github.io/jfc/api/#io.jfc.auto.package
[jfc-jawn]: https://travisbrown.github.io/jfc/api/#io.jfc.jawn.package
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
