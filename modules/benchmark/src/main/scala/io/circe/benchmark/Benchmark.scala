package io.circe.benchmark

import cats.Eq
import cats.data.NonEmptyList
import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.semiauto._
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

case class Foo(s: String, d: Double, i: Int, l: Long, bs: List[Boolean])

object Foo {
  implicit val decodeFoo: Decoder[Foo] = deriveDecoder
  implicit val encodeFoo: Encoder[Foo] = deriveEncoder

  implicit val eqFoo: Eq[Foo] = Eq.fromUniversalEquals[Foo]
}

case class FooNel(s: String, d: Double, i: Int, l: Long, bs: NonEmptyList[Boolean])

object FooNel {
  implicit val encodeFooNel: Encoder[FooNel] = deriveEncoder
}

class ExampleData {
  val ints: List[Int] = (0 to 1000).toList

  val foos: Map[String, Foo] = List.tabulate(100) { i =>
    ("b" * i) -> Foo("a" * i, (i + 2.0) / (i + 1.0), i, i * 1000L, (0 to i).map(_ % 2 == 0).toList)
  }.toMap

  val fooNels: Map[String, FooNel] = foos.mapValues {
    foo => FooNel(foo.s, foo.d, foo.i, foo.l, NonEmptyList(true, foo.bs))
  }

  @inline def encodeC[A](a: A)(implicit encode: Encoder[A]): Json = encode(a)

  val intsC: Json = encodeC(ints)

  val foosC: Json = encodeC(foos)
  val fooNelsC: Json = encodeC(fooNels)

  val intsJson: String = intsC.noSpaces
  val foosJson: String = foosC.noSpaces
}


/**
 * Compare the performance of derived and non-derived codecs.
 *
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmark/jmh:run -i 10 -wi 10 -f 2 -t 1 io.circe.benchmark.CirceDerivationBenchmark"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class CirceDerivationBenchmark {
  import io.circe._

  private[this] val derivedDecoder: Decoder[Foo] = deriveDecoder
  private[this] val derivedEncoder: Encoder[Foo] = deriveEncoder

  private[this] val nonDerivedDecoder: Decoder[Foo] = new Decoder[Foo] {
    def apply(c: HCursor): Decoder.Result[Foo] = for {
      s <- c.get[String]("s").right
      d <- c.get[Double]("d").right
      i <- c.get[Int]("i").right
      l <- c.get[Long]("l").right
      bs <- c.get[List[Boolean]]("bs").right
    } yield Foo(s, d, i, l, bs)
  }

  private[this] val nonDerivedEncoder: Encoder[Foo] = new Encoder[Foo] {
    def apply(foo: Foo): Json = Json.obj(
      "s" -> Encoder[String].apply(foo.s),
      "d" -> Encoder[Double].apply(foo.d),
      "i" -> Encoder[Int].apply(foo.i),
      "l" -> Encoder[Long].apply(foo.l),
      "bs" -> Encoder[List[Boolean]].apply(foo.bs)
    )
  }

  val exampleFoo: Foo = Foo(
    "abcdefghijklmnopqrstuvwxyz",
    1001.0,
    2002,
    3003L,
    List(true, false, true, false, true, false, true, false, true)
  )

  val exampleFooJson = Encoder[Foo].apply(exampleFoo)

  @Benchmark
  def decodeDerived: Decoder.Result[Foo] = derivedDecoder.decodeJson(exampleFooJson)

  @Benchmark
  def decodeNonDerived: Decoder.Result[Foo] = nonDerivedDecoder.decodeJson(exampleFooJson)

  @Benchmark
  def encodeDerived: Json = derivedEncoder(exampleFoo)

  @Benchmark
  def encodeNonDerived: Json = nonDerivedEncoder(exampleFoo)
}
