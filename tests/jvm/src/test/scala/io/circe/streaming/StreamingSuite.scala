package io.circe.streaming

import cats.Eval
import cats.data.{ Xor, XorT }
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.tests.CirceSuite
import io.circe.tests.examples._
import io.iteratee.{ Enumeratee, Enumerator }
import io.iteratee.xor._

class StreamingSuite extends CirceSuite {
  type Result[A] = XorT[Eval, Throwable, A]

  def enumerateFoos(fooStream: Stream[Foo], fooVector: Vector[Foo]): Enumerator[Result, Foo] =
    enumStream(fooStream).append(enumVector(fooVector))

  def serializeFoos(foos: Enumerator[Result, Foo]): Enumerator[Result, String] =
    enumOne("[").append(foos.mapE(map((_: Foo).asJson.spaces2).andThen(intersperse(", ")))).append(enumOne("]"))

  def stringToBytes: Enumeratee[Result, String, Array[Byte]] = map(
    _.getBytes(java.nio.charset.Charset.forName("UTF-8"))
  )

  test("stringParser") {
    check { (fooStream: Stream[Foo], fooVector: Vector[Foo]) =>
      val enumerator = serializeFoos(enumerateFoos(fooStream, fooVector))
      val foos = (fooStream ++ fooVector).map(_.asJson)
      enumerator.mapE(stringParser).toVector.value.value === Xor.right(foos.toVector)
    }
  }

  test("byteParser") {
    check { (fooStream: Stream[Foo], fooVector: Vector[Foo]) =>
      val enumerator = serializeFoos(enumerateFoos(fooStream, fooVector)).mapE(stringToBytes)
      val foos = (fooStream ++ fooVector).map(_.asJson)
      enumerator.mapE(byteParser).toVector.value.value === Xor.right(foos.toVector)
    }
  }

  test("decoder") {
    check { (fooStream: Stream[Foo], fooVector: Vector[Foo]) =>
      val enumerator = serializeFoos(enumerateFoos(fooStream, fooVector))
      val foos = fooStream ++ fooVector
      enumerator.mapE(stringParser).mapE(decoder[Result, Foo]).toVector.value.value === Xor.right(foos.toVector)
    }
  }
}
