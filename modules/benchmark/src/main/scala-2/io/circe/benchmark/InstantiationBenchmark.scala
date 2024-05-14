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
import org.openjdk.jmh.annotations._

/**
 * Compare the performance of various ways of folding JSON values.
 *
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmark/jmh:run -i 10 -wi 10 -f 2 -t 1 io.circe.benchmark.DecoderInstantiationBenchmark"
 */
@State(Scope.Thread)
class InstantiationBenchmark {
  val input = Json.obj("value" -> Json.fromString("xyz"))

  @Benchmark
  def decoderFromNew: Decoder.Result[String] = {
    val decoder: Decoder[String] = new Decoder[String] {
      final def apply(c: HCursor): Decoder.Result[String] =
        c.as[String].left.flatMap(_ => c.get[String]("value"))
    }

    decoder.decodeJson(input)
  }

  @Benchmark
  def decoderFromSAM: Decoder.Result[String] = {
    val decoder: Decoder[String] = (c: HCursor) => c.as[String].left.flatMap(_ => c.get[String]("value"))

    decoder.decodeJson(input)
  }

  @Benchmark
  def decoderFromInstance: Decoder.Result[String] = {
    val decoder: Decoder[String] =
      Decoder.instance[String]((c: HCursor) => c.as[String].left.flatMap(_ => c.get[String]("value")))

    decoder.decodeJson(input)
  }

  @Benchmark
  def encoderFromNew: Json = {
    val encoder: Encoder[String] = new Encoder[String] {
      def apply(s: String): Json = Json.obj("value" -> Json.fromString(s))
    }

    encoder("abc")
  }

  @Benchmark
  def encoderFromSAM: Json = {
    val encoder: Encoder[String] =
      (s: String) => Json.obj("value" -> Json.fromString(s))

    encoder("abc")
  }

  @Benchmark
  def encoderFromInstance: Json = {
    val encoder: Encoder[String] = Encoder.instance[String]((s: String) => Json.obj("value" -> Json.fromString(s)))

    encoder("abc")
  }
}
