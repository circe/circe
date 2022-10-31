package io.circe

import cats.syntax.eq._
import io.circe.tests.CirceMunitSuite
import io.circe.syntax.EncoderOps
import cats.instances.int.catsKernelStdOrderForInt
import cats.instances.string.catsKernelStdOrderForString
import io.circe.testing.CodecTests
import org.scalacheck.Prop._
import scala.collection.immutable.ArraySeq

class ArraySeqSuite extends CirceMunitSuite {

  def decodeArraySeqWithoutClassTag[A: Decoder](json: Json): Decoder.Result[ArraySeq[A]] =
    json.as[ArraySeq[A]]

  property("decoding an arraySeq should succeed when the type is fully specified") {
    forAll { int: Int =>
      Json.arr(int.asJson).as[ArraySeq[Int]] ?= Right(ArraySeq(int))
    }
  }

  property("decoding an arraySeq should succeed for polymorphic decoders") {
    forAll { string: String =>
      decodeArraySeqWithoutClassTag[String](Json.arr(string.asJson)) ?= Right(ArraySeq(string))
    }
  }

  property("decoding an arraySeq should specialise the array type where a class tag is available") {
    forAll { intArray: Array[Int] =>
      val jsonArray = Json.arr(intArray.map(_.asJson): _*)

      jsonArray.as[ArraySeq[Int]].map(_.getClass) ?= Right(classOf[ArraySeq.ofInt])
    }
  }

  property("decoding an arraySeq should not specialise the array type where no class tag is available") {
    forAll { intArray: Array[Int] =>
      val jsonArray = Json.arr(intArray.map(_.asJson): _*)

      decodeArraySeqWithoutClassTag[Int](jsonArray).map(_.getClass) ?= Right(
        classOf[ArraySeq.ofRef[_]].asInstanceOf[Class[_ <: ArraySeq[Int]]]
      )
    }
  }

  checkAll("Codec[ArraySeq[Int]]", CodecTests[ArraySeq[Int]].codec)
  checkAll("Codec[ArraySeq[String]]", CodecTests[ArraySeq[String]].codec)

}
