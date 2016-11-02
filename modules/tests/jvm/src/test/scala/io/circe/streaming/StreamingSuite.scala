package io.circe.streaming

import cats.Eval
import cats.data.EitherT
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.tests.CirceSuite
import io.circe.tests.examples._
import io.iteratee.{ Enumeratee, Enumerator }
import io.iteratee.modules.eitherT._

class StreamingSuite extends CirceSuite {
  type Result[A] = EitherT[Eval, Throwable, A]

  def enumerateFoos(fooStream: Stream[Foo], fooVector: Vector[Foo]): Enumerator[Result, Foo] =
    enumStream(fooStream).append(enumVector(fooVector))

  def serializeFoos(foos: Enumerator[Result, Foo]): Enumerator[Result, String] =
    enumOne("[").append(foos.through(map((_: Foo).asJson.spaces2).andThen(intersperse(", ")))).append(enumOne("]"))

  def stringToBytes: Enumeratee[Result, String, Array[Byte]] = map(
    _.getBytes(java.nio.charset.Charset.forName("UTF-8"))
  )

  "stringParser" should "parse enumerated lines" in forAll { (fooStream: Stream[Foo], fooVector: Vector[Foo]) =>
    val enumerator = serializeFoos(enumerateFoos(fooStream, fooVector))
    val foos = (fooStream ++ fooVector).map(_.asJson)

    assert(enumerator.through(stringParser).toVector.value.value === Right(foos.toVector))
  }

  "byteParser" should "parse enumerated bytes" in forAll { (fooStream: Stream[Foo], fooVector: Vector[Foo]) =>
    val enumerator = serializeFoos(enumerateFoos(fooStream, fooVector)).through(stringToBytes)
    val foos = (fooStream ++ fooVector).map(_.asJson)

    assert(enumerator.through(byteParser).toVector.value.value === Right(foos.toVector))
  }

  "decoder" should "decode enumerated JSON values" in forAll { (fooStream: Stream[Foo], fooVector: Vector[Foo]) =>
    val foos = fooStream ++ fooVector
    val foosEnumerator = serializeFoos(enumerateFoos(fooStream, fooVector))
    val enumerator = foosEnumerator.through(stringParser).through(decoder[Result, Foo])

    assert(enumerator.toVector.value.value === Right(foos.toVector))
  }
}
