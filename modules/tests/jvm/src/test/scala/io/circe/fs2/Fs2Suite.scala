package io.circe.fs2

import _root_.fs2.{ Stream, Task, text }
import io.circe.{ DecodingFailure, ParsingFailure }
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.tests.CirceSuite
import io.circe.tests.examples._
import scala.collection.immutable.{ Stream => StdStream }

class Fs2Suite extends CirceSuite {
  def fooStream(fooStdStream: StdStream[Foo], fooVector: Vector[Foo]): Stream[Task, Foo] =
    Stream.emits(fooStdStream).append(Stream.emits(fooVector))

  def serializeFoos(foos: Stream[Task, Foo]): Stream[Task, String] =
    Stream("[").append(foos.map((_: Foo).asJson.spaces2).intersperse(", ")).append(Stream("]"))

  def stringStream(stringStdStream: StdStream[String], stringVector: Vector[String]): Stream[Task, String] =
    Stream.emits(stringStdStream).append(Stream.emits(stringVector))

  "stringParser" should "parse lines stream" in forAll { (fooStdStream: StdStream[Foo], fooVector: Vector[Foo]) =>
    val stream = serializeFoos(fooStream(fooStdStream, fooVector))
    val foos = (fooStdStream ++ fooVector).map(_.asJson)

    assert(stream.through(stringParser).runLog.unsafeAttemptRun === Right(foos.toVector))
  }

  "byteParser" should "parse enumerated bytes" in forAll { (fooStdStream: StdStream[Foo], fooVector: Vector[Foo]) =>
    val stream = serializeFoos(fooStream(fooStdStream, fooVector)).through(text.utf8EncodeC)
    val foos = (fooStdStream ++ fooVector).map(_.asJson)

    assert(stream.through(byteParser).runLog.unsafeAttemptRun === Right(foos.toVector))
  }

  "decoder" should "decode enumerated JSON values" in forAll { (fooStdStream: StdStream[Foo], fooVector: Vector[Foo]) =>
    val stream = serializeFoos(fooStream(fooStdStream, fooVector))
    val foos = fooStdStream ++ fooVector

    assert(stream.through(stringParser).through(decoder[Task, Foo]).runLog.unsafeAttemptRun === Right(foos.toVector))
  }

  "stringParser" should "return ParsingFailure" in
  forAll { (stringStdStream: StdStream[String], stringVector: Vector[String]) =>
    val result = Stream("}").append(stringStream(stringStdStream, stringVector))
      .through(stringParser)
      .runLog.unsafeAttemptRun
    assert(result.isLeft && result.left.get.isInstanceOf[ParsingFailure])
  }

  "byteParser" should "return ParsingFailure" in
  forAll { (stringStdStream: StdStream[String], stringVector: Vector[String]) =>
    val result = Stream("}").append(stringStream(stringStdStream, stringVector))
      .through(text.utf8EncodeC).through(byteParser)
      .runLog.unsafeAttemptRun
    assert(result.isLeft && result.left.get.isInstanceOf[ParsingFailure])
  }

  "decoder" should "return DecodingFailure" in
  forAll { (fooStdStream: StdStream[Foo], fooVector: Vector[Foo]) =>
    sealed trait Foo2
    case class Bar2(x: String) extends Foo2

    whenever(fooStdStream.nonEmpty && fooVector.nonEmpty) {
      val result = serializeFoos(fooStream(fooStdStream, fooVector))
        .through(stringParser).through(decoder[Task, Foo2])
        .runLog.unsafeAttemptRun

      assert(result.isLeft && result.left.get.isInstanceOf[DecodingFailure])
    }
  }
}
