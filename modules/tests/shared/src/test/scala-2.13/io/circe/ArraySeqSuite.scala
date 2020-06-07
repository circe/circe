package io.circe

import cats.Eq
import io.circe.tests.CirceSuite
import io.circe.syntax.EncoderOps
import cats.instances.list.catsKernelStdEqForList
import cats.instances.int.catsKernelStdOrderForInt
import cats.instances.string.catsKernelStdOrderForString
import io.circe.testing.CodecTests

import scala.collection.immutable.ArraySeq

class ArraySeqSuite extends CirceSuite {

  // TODO this can be removed once Cats 2.2.0 is used
  implicit def eqForArraySeq[A: Eq]: Eq[ArraySeq[A]] = Eq.by(_.toList)

  def decodeArraySeqWithoutClassTag[A: Decoder](json: Json): Decoder.Result[ArraySeq[A]] =
    json.as[ArraySeq[A]]

  "decoding an arraySeq" should "succeed when the type is fully specified" in forAll { int: Int =>
    assert(Json.arr(int.asJson).as[ArraySeq[Int]] === Right(ArraySeq(int)))
  }

  it should "succeed for polymorphic decoders" in forAll { string: String =>
    assert(decodeArraySeqWithoutClassTag[String](Json.arr(string.asJson)) === Right(ArraySeq(string)))
  }

  it should "specialise the array type where a class tag is available" in forAll { intArray: Array[Int] =>
    val jsonArray = Json.arr(intArray.map(_.asJson): _*)

    assert(jsonArray.as[ArraySeq[Int]].map(_.getClass) == Right(classOf[ArraySeq.ofInt]))
  }

  it should "not specialise the array type where no class tag is available" in forAll { intArray: Array[Int] =>
    val jsonArray = Json.arr(intArray.map(_.asJson): _*)

    assert(decodeArraySeqWithoutClassTag[Int](jsonArray).map(_.getClass) == Right(classOf[ArraySeq.ofRef[_]]))
  }

  checkAll("Codec[ArraySeq[Int]]", CodecTests[ArraySeq[Int]].codec)
  checkAll("Codec[ArraySeq[String]]", CodecTests[ArraySeq[String]].codec)

}
