package io.circe.streaming

import _root_.jawn.AsyncParser
import cats.Eval
import cats.data.EitherT
import io.circe._
import io.circe.syntax._
import io.circe.tests.CirceSuite
import io.circe.tests.examples._
import io.iteratee.{Enumeratee, Enumerator}
import io.iteratee.modules.eitherT._

class StreamingSuite extends CirceSuite {
  type Result[A] = EitherT[Eval, Throwable, A]

  implicit val decodeFoo: Decoder[Foo] = Foo.decodeFoo
  implicit val encodeFoo: Encoder[Foo] = Foo.encodeFoo

  def enumerateFoos(fooStream: Stream[Foo], fooVector: Vector[Foo]): Enumerator[Result, Foo] =
    enumStream(fooStream).append(enumVector(fooVector))

  def serializeFoos(parsingMode: AsyncParser.Mode, foos: Enumerator[Result, Foo]): Enumerator[Result, String] =
    parsingMode match {
      case AsyncParser.ValueStream =>
        foos.through(map((_: Foo).asJson.spaces2).andThen(intersperse("\n")))
      case AsyncParser.UnwrapArray =>
        enumOne("[").append(foos.through(map((_: Foo).asJson.spaces2).andThen(intersperse(", ")))).append(enumOne("]"))
      case _ => ???
    }

  def stringToBytes: Enumeratee[Result, String, Array[Byte]] = map(
    _.getBytes(java.nio.charset.Charset.forName("UTF-8"))
  )

  "stringArrayParser" should "parse values wrapped in array" in {
    testParser(AsyncParser.UnwrapArray, stringArrayParser)
  }

  "byteArrayParser" should "parse bytes wrapped in array" in {
    testParser(AsyncParser.UnwrapArray, stringToBytes.andThen(byteArrayParser))
  }

  "stringStreamParser" should "parse values delimeted by new lines" in {
    testParser(AsyncParser.ValueStream, stringStreamParser)
  }

  "byteStreamParser" should "parse bytes delimeted by new lines" in {
    testParser(AsyncParser.ValueStream,  stringToBytes.andThen(byteStreamParser))
  }

  "decoder" should "decode enumerated JSON values" in forAll { (fooStream: Stream[Foo], fooVector: Vector[Foo]) =>
    val enumerator = serializeFoos(AsyncParser.UnwrapArray, enumerateFoos(fooStream, fooVector))
    val foos = fooStream ++ fooVector

    assert(enumerator.through(stringArrayParser).through(decoder[Result, Foo]).toVector.value.value === Right(foos.toVector))
  }

  private def testParser(mode: AsyncParser.Mode, through: Enumeratee[Result, String, Json]) = {
    forAll { (fooStream: Stream[Foo], fooVector: Vector[Foo]) =>
      val enumerator = serializeFoos(mode, enumerateFoos(fooStream, fooVector)).through(through)
      val foos = (fooStream ++ fooVector).map(_.asJson).toVector

      assert(enumerator.toVector.value.value === Right(foos.toVector))
    }
  }
}
