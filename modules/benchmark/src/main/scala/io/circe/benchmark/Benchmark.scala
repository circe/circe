package io.circe.benchmark

import argonaut.{ Json => JsonA }
import io.circe.{ Json => JsonC }
import io.github.netvl.picopickle.backends.jawn.JsonPickler
import io.github.netvl.picopickle.backends.jawn.JsonPickler._
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._
import play.api.libs.json.{ JsValue => JsValueP }
import spray.json.{ JsValue => JsValueS }
import spray.json.DefaultJsonProtocol._

class ExampleData {
  val ints: List[Int] = (0 to 1000).toList

  val foos: Map[String, Foo] = List.tabulate(100) { i =>
    ("b" * i) -> Foo("a" * i, (i + 2.0) / (i + 1.0), i, i * 1000L, (0 to i).map(_ % 2 == 0).toList)
  }.toMap

  @inline def encodeA[A](a: A)(implicit encode: argonaut.EncodeJson[A]): JsonA = encode(a)
  @inline def encodeC[A](a: A)(implicit encode: io.circe.Encoder[A]): JsonC = encode(a)
  @inline def encodeP[A](a: A)(implicit encode: play.api.libs.json.Writes[A]): JsValueP = encode.writes(a)
  @inline def encodeS[A](a: A)(implicit encode: spray.json.JsonWriter[A]): JsValueS = encode.write(a)
  @inline def encodePico[A](a: A)(implicit encode: JsonPickler.Writer[A]): backend.BValue = write(a)(encode)

  val intsC: JsonC = encodeC(ints)
  val intsA: JsonA = encodeA(ints)
  val intsP: JsValueP = encodeP(ints)
  val intsS: JsValueS = encodeS(ints)
  val intsPico: backend.BValue = encodePico(ints)

  val foosC: JsonC = encodeC(foos)
  val foosA: JsonA = encodeA(foos)
  val foosP: JsValueP = encodeP(foos)
  val foosS: JsValueS = encodeS(foos)
  val foosPico: backend.BValue = encodePico(foos)

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
  def encodeIntsPico: backend.BValue = encodePico(ints)

  @Benchmark
  def encodeFoosC: JsonC = encodeC(foos)

  @Benchmark
  def encodeFoosA: JsonA = encodeA(foos)

  @Benchmark
  def encodeFoosP: JsValueP = encodeP(foos)

  @Benchmark
  def encodeFoosS: JsValueS = encodeS(foos)

  @Benchmark
  def encodeFoosPico: backend.BValue = encodePico(foos)
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
  def decodeIntsC: List[Int] = intsC.as[List[Int]].right.getOrElse(throw new Exception)

  @Benchmark
  def decodeIntsA: List[Int] = intsA.as[List[Int]].result.right.getOrElse(throw new Exception)

  @Benchmark
  def decodeIntsP: List[Int] = intsP.as[List[Int]]

  @Benchmark
  def decodeIntsS: List[Int] = intsS.convertTo[List[Int]]

  @Benchmark
  def decodeIntsPico: List[Int] = read[List[Int]](intsPico)

  @Benchmark
  def decodeFoosC: Map[String, Foo] =
    foosC.as[Map[String, Foo]].right.getOrElse(throw new Exception)

  @Benchmark
  def decodeFoosA: Map[String, Foo] =
    foosA.as[Map[String, Foo]].result.right.getOrElse(throw new Exception)

  @Benchmark
  def decodeFoosP: Map[String, Foo] = foosP.as[Map[String, Foo]]

  @Benchmark
  def decodeFoosS: Map[String, Foo] = foosS.convertTo[Map[String, Foo]]

  @Benchmark
  def decodeFoosPico: Map[String, Foo] = read[Map[String, Foo]](foosPico)
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
  def parseIntsC: JsonC = io.circe.jawn.parse(intsJson).right.getOrElse(throw new Exception)

  @Benchmark
  def parseIntsCJ: JsonC = io.circe.jackson.parse(intsJson).right.getOrElse(throw new Exception)

  @Benchmark
  def parseIntsA: JsonA = argonaut.Parse.parse(intsJson).right.getOrElse(throw new Exception)

  @Benchmark
  def parseIntsP: JsValueP = play.api.libs.json.Json.parse(intsJson)

  @Benchmark
  def parseIntsS: JsValueS = spray.json.JsonParser(intsJson)

  @Benchmark
  def parseIntsPico: backend.BValue = readAst(intsJson)

  @Benchmark
  def parseFoosC: JsonC = io.circe.jawn.parse(foosJson).right.getOrElse(throw new Exception)

  @Benchmark
  def parseFoosCJ: JsonC = io.circe.jackson.parse(foosJson).right.getOrElse(throw new Exception)

  @Benchmark
  def parseFoosA: JsonA = argonaut.Parse.parse(foosJson).right.getOrElse(throw new Exception)

  @Benchmark
  def parseFoosP: JsValueP = play.api.libs.json.Json.parse(foosJson)

  @Benchmark
  def parseFoosS: JsValueS = spray.json.JsonParser(foosJson)

  @Benchmark
  def parseFoosPico: backend.BValue = readAst(foosJson)
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
  def printIntsP: String = play.api.libs.json.Json.stringify(intsP)

  @Benchmark
  def printIntsS: String = intsS.compactPrint

  @Benchmark
  def printIntsPico: String = writeAst(intsPico)

  @Benchmark
  def printFoosC: String = foosC.noSpaces

  @Benchmark
  def printFoosCJ: String = io.circe.jackson.jacksonPrint(foosC)

  @Benchmark
  def printFoosA: String = foosA.nospaces

  @Benchmark
  def printFoosP: String = play.api.libs.json.Json.stringify(foosP)

  @Benchmark
  def printFoosS: String = foosS.compactPrint

  @Benchmark
  def printFoosPico: String = writeAst(foosPico)
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
  import io.circe.generic.semiauto._

  private[this] val derivedDecoder: Decoder[Foo] = deriveDecoder
  private[this] val derivedEncoder: Encoder[Foo] = deriveEncoder

  private[this] val nonDerivedDecoder: Decoder[Foo] = Foo.circeDecodeFoo
  private[this] val nonDerivedEncoder: Encoder[Foo] = Foo.circeEncodeFoo

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
