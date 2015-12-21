package io.circe.benchmark

import algebra.Eq
import argonaut.{ Json => JsonA, _ }, argonaut.Argonaut._
import io.circe.{ Decoder, Encoder, Json => JsonC }
import io.circe.generic.semiauto._
import io.circe.jawn._
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._
import play.api.libs.json.{ Format, Json => JsonP, JsValue => JsValueP, Writes }
import spray.json.{ JsonFormat, JsonParser => JsonParserS, JsonWriter, JsValue => JsValueS }
import spray.json.DefaultJsonProtocol._

case class Foo(s: String, d: Double, i: Int, l: Long, bs: List[Boolean])

object Foo {
  implicit val codecFoo: CodecJson[Foo] = CodecJson.derive[Foo]
  implicit val playFormatFoo: Format[Foo] = JsonP.format[Foo]
  implicit val decodeFoo: Decoder[Foo] = deriveDecoder
  implicit val encodeFoo: Encoder[Foo] = deriveEncoder
  implicit val sprayFormatFoo: JsonFormat[Foo] = jsonFormat5(Foo.apply)

  implicit val eqFoo: Eq[Foo] = Eq.fromUniversalEquals[Foo]
}

class ExampleData {
  val ints: List[Int] = (0 to 1000).toList

  val foos: Map[String, Foo] = List.tabulate(100) { i =>
    ("b" * i) -> Foo("a" * i, (i + 2.0) / (i + 1.0), i, i * 1000L, (0 to i).map(_ % 2 == 0).toList)
  }.toMap

  @inline def encodeA[A](a: A)(implicit encode: EncodeJson[A]): JsonA = encode(a)
  @inline def encodeC[A](a: A)(implicit encode: Encoder[A]): JsonC = encode(a)
  @inline def encodeP[A](a: A)(implicit encode: Writes[A]): JsValueP = encode.writes(a)
  @inline def encodeS[A](a: A)(implicit encode: JsonWriter[A]): JsValueS = encode.write(a)

  val intsC: JsonC = encodeC(ints)
  val intsA: JsonA = encodeA(ints)
  val intsP: JsValueP = encodeP(ints)
  val intsS: JsValueS = encodeS(ints)

  val foosC: JsonC = encodeC(foos)
  val foosA: JsonA = encodeA(foos)
  val foosP: JsValueP = encodeP(foos)
  val foosS: JsValueS = encodeS(foos)

  val intsJson: String = intsC.noSpaces
  val foosJson: String = foosC.noSpaces
}

/**
 * Compare the performance of encoding operations.
 *
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmark/jmh:run -i 10 -wi 10 -f 2 -t 1 io.circe.benchmark.EncodingBenchmark"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class EncodingBenchmark extends ExampleData {
  @Benchmark
  def encodeIntsC: JsonC = encodeC(ints)

  @Benchmark
  def encodeIntsA: JsonA = encodeA(ints)

  @Benchmark
  def encodeIntsP: JsValueP = encodeP(ints)

  @Benchmark
  def encodeIntsS: JsValueS = encodeS(ints)

  @Benchmark
  def encodeFoosC: JsonC = encodeC(foos)

  @Benchmark
  def encodeFoosA: JsonA = encodeA(foos)

  @Benchmark
  def encodeFoosP: JsValueP = encodeP(foos)

  @Benchmark
  def encodeFoosS: JsValueS = encodeS(foos)
}

/**
 * Compare the performance of decoding operations.
 *
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmark/jmh:run -i 10 -wi 10 -f 2 -t 1 io.circe.benchmark.DecodingBenchmark"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class DecodingBenchmark extends ExampleData {
  @Benchmark
  def decodeIntsC: List[Int] = intsC.as[List[Int]].getOrElse(throw new Exception)

  @Benchmark
  def decodeIntsA: List[Int] = intsA.as[List[Int]].result.getOrElse(throw new Exception)

  @Benchmark
  def decodeIntsP: List[Int] = intsP.as[List[Int]]

  @Benchmark
  def decodeIntsS: List[Int] = intsS.convertTo[List[Int]]

  @Benchmark
  def decodeFoosC: Map[String, Foo] =
    foosC.as[Map[String, Foo]].getOrElse(throw new Exception)

  @Benchmark
  def decodeFoosA: Map[String, Foo] =
    foosA.as[Map[String, Foo]].result.getOrElse(throw new Exception)

  @Benchmark
  def decodeFoosP: Map[String, Foo] = foosP.as[Map[String, Foo]]

  @Benchmark
  def decodeFoosS: Map[String, Foo] = foosS.convertTo[Map[String, Foo]]
}

/**
 * Compare the performance of parsing operations.
 *
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmark/jmh:run -i 10 -wi 10 -f 2 -t 1 io.circe.benchmark.ParsingBenchmark"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class ParsingBenchmark extends ExampleData {
  @Benchmark
  def parseIntsC: JsonC = parse(intsJson).getOrElse(throw new Exception)

  @Benchmark
  def parseIntsCJ: JsonC = io.circe.jackson.parse(intsJson).getOrElse(throw new Exception)

  @Benchmark
  def parseIntsA: JsonA = Parse.parse(intsJson).getOrElse(throw new Exception)

  @Benchmark
  def parseIntsP: JsValueP = JsonP.parse(intsJson)

  @Benchmark
  def parseIntsS: JsValueS = JsonParserS(intsJson)

  @Benchmark
  def parseFoosC: JsonC = parse(foosJson).getOrElse(throw new Exception)

  @Benchmark
  def parseFoosCJ: JsonC = io.circe.jackson.parse(foosJson).getOrElse(throw new Exception)

  @Benchmark
  def parseFoosA: JsonA = Parse.parse(foosJson).getOrElse(throw new Exception)

  @Benchmark
  def parseFoosP: JsValueP = JsonP.parse(foosJson)

  @Benchmark
  def parseFoosS: JsValueS = JsonParserS(foosJson)
}

/**
 * Compare the performance of printing operations.
 *
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmark/jmh:run -i 10 -wi 10 -f 2 -t 1 io.circe.benchmark.PrintingBenchmark"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class PrintingBenchmark extends ExampleData {
  @Benchmark
  def printIntsC: String = intsC.noSpaces

  @Benchmark
  def printIntsCJ: String = io.circe.jackson.jacksonPrint(intsC)

  @Benchmark
  def printIntsA: String = intsA.nospaces

  @Benchmark
  def printIntsP: String = JsonP.stringify(intsP)

  @Benchmark
  def printIntsS: String = intsS.compactPrint

  @Benchmark
  def printFoosC: String = foosC.noSpaces

  @Benchmark
  def printFoosCJ: String = io.circe.jackson.jacksonPrint(foosC)

  @Benchmark
  def printFoosA: String = foosA.nospaces

  @Benchmark
  def printFoosP: String = JsonP.stringify(foosP)

  @Benchmark
  def printFoosS: String = foosS.compactPrint
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
      s <- c.get[String]("s")
      d <- c.get[Double]("d")
      i <- c.get[Int]("i")
      l <- c.get[Long]("l")
      bs <- c.get[List[Boolean]]("bs")
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
