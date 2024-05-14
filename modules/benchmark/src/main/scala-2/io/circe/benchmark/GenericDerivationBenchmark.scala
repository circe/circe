/*
 * Copyright 2024 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe.benchmark

import io.circe.{ Decoder, Encoder, HCursor, Json }
import io.circe.generic.semiauto._
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

/**
 * Compare the performance of derived and non-derived codecs.
 *
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmark/jmh:run -i 10 -wi 10 -f 2 -t 1 io.circe.benchmark.GenericDerivationBenchmark"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class GenericDerivationBenchmark {
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
