package io.circe.benchmark

import cats.kernel.Eq
import io.circe.{ Codec, Decoder, Encoder, HCursor, Json }
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

case class Foo(s: String, d: Double, i: Int, l: Long, bs: List[Boolean]) derives Codec.AsObject

object Foo {
  implicit val eqFoo: Eq[Foo] = Eq.fromUniversalEquals[Foo]
}

/**
 * Compare the performance of derived and non-derived codecs.
 *
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmarkDotty/jmh:run -i 10 -wi 10 -f 2 -t 1 io.circe.benchmark.DerivesBenchmark"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class DerivesBenchmark {
  private[this] val nonDerivedCodec: Codec.AsObject[Foo] =
    Codec.forProduct5("s", "d", "i", "l", "bs")(Foo.apply) {
      case Foo(s, d, i, l, bs) => (s, d, i, l, bs)
    }

  val exampleFoo: Foo = Foo(
    "abcdefghijklmnopqrstuvwxyz",
    1001.0,
    2002,
    3003L,
    List(true, false, true, false, true, false, true, false, true)
  )

  val derivedCodec = Codec[Foo]
  val exampleFooJson = derivedCodec.apply(exampleFoo)

  @Benchmark
  def decodeDerived: Decoder.Result[Foo] = derivedCodec.decodeJson(exampleFooJson)

  @Benchmark
  def decodeNonDerived: Decoder.Result[Foo] = nonDerivedCodec.decodeJson(exampleFooJson)

  @Benchmark
  def encodeDerived: Json = derivedCodec.apply(exampleFoo)

  @Benchmark
  def encodeNonDerived: Json = nonDerivedCodec(exampleFoo)
}
