package io.circe.benchmark

import io.circe.{ Decoder, Encoder, Error, HCursor, Json }
import io.circe.flat.FlatCursor
import io.circe.generic.semiauto._
import io.circe.jawn
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

/**
 * Compare the performance of derived and non-derived codecs.
 *
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmark/jmh:run -i 10 -wi 10 -f 2 -t 1 io.circe.benchmark.DecodingBenchmark"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class DecodingBenchmark {
  private[this] val derivedDecoder: Decoder[Foo] = deriveDecoder

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
  val exampleFooString = exampleFooJson.spaces2

  val example = """
  {
  "created_at": "Thu Apr 06 15:24:15 +0000 2017",
  "id_str": "850006245121695744",
  "text": "1\/ Today we\u2019re sharing our vision for the future of the Twitter API platform!\nhttps:\/\/t.co\/XweGngmxlP",
  "user": {
    "id": 2244994945,
    "name": "Twitter Dev",
    "screen_name": "TwitterDev",
    "location": "Internet",
    "url": "https:\/\/dev.twitter.com\/",
    "description": "Your official source for Twitter Platform news, updates & events. Need technical help? Visit https:\/\/twittercommunity.com\/ \u2328\ufe0f #TapIntoTwitter"
  },
  "place": {   
  },
  "entities": {
    "hashtags": [      
    ],
    "urls": [
      {
        "url": "https:\/\/t.co\/XweGngmxlP",
        "unwound": {
          "url": "https:\/\/cards.twitter.com\/cards\/18ce53wgo4h\/3xo1c",
          "title": "Building the Future of the Twitter API Platform"
        }
      }
    ],
    "user_mentions": [     
    ]
  }
}
"""

  import io.circe.syntax._

  //val intsExample = (0 to 1000).toVector.asJson.spaces2

  @Benchmark
  def decodeJawn: Either[Error, Foo] = jawn.decode[Foo](exampleFooString)(derivedDecoder)
  //def decodeJawn: Either[io.circe.ParsingFailure, Json] = jawn.parse(example)
  //def decodeJawn: Either[io.circe.ParsingFailure, Json] = jawn.parse(exampleFooString)
  //def decodeJawn: Either[Error, List[Long]] = jawn.decode[List[Long]](intsExample)

  @Benchmark
  def decodeFlat: Either[Error, Foo] = FlatCursor.decode[Foo](exampleFooString)(derivedDecoder)
  //def decodeFlat: Either[io.circe.flat.ParseFailure, FlatCursor] = FlatCursor.parse(example)
  //def decodeFlat: Either[io.circe.flat.ParseFailure, FlatCursor] = FlatCursor.parse(exampleFooString)
  //def decodeFlat: Either[Error, List[Long]] = FlatCursor.decode[List[Long]](intsExample)

  /*
  @Benchmark
  def decodeNonDerived: Decoder.Result[Foo] = nonDerivedDecoder.decodeJson(exampleFooJson)

  @Benchmark
  def encodeDerived: Json = derivedEncoder(exampleFoo)

  @Benchmark
  def encodeNonDerived: Json = nonDerivedEncoder(exampleFoo)
   */
}
