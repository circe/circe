package io.circe

import java.util.UUID

import algebra.Eq
import cats.data._
import cats.laws.discipline.arbitrary._
import io.circe.tests.{ CodecTests, CirceSuite }

class AnyValCodecSuite extends CirceSuite {
  /**
   * We provide a special [[algebra.Eq]] instance for [[scala.Float]] that does not distinguish
   * `NaN` from itself.
   */
  val eqFloat: Eq[Float] = Eq.instance { (a, b) =>
    (a.isNaN && b.isNaN) || cats.std.float.floatAlgebra.eqv(a, b)
  }

  /**
   * We provide a special [[algebra.Eq]] instance for [[scala.Double]] that does not distinguish
   * `NaN` from itself.
   */
  val eqDouble: Eq[Double] = Eq.instance { (a, b) =>
    (a.isNaN && b.isNaN) || cats.std.double.doubleAlgebra.eqv(a, b)
  }

  checkAll("Codec[Unit]", CodecTests[Unit].codec)
  checkAll("Codec[Boolean]", CodecTests[Boolean].codec)
  checkAll("Codec[Char]", CodecTests[Char].codec)
  checkAll("Codec[Float]", CodecTests[Float].codec(implicitly, eqFloat))
  checkAll("Codec[Double]", CodecTests[Double].codec(implicitly, eqDouble))
  checkAll("Codec[Byte]", CodecTests[Byte].codec)
  checkAll("Codec[Short]", CodecTests[Short].codec)
  checkAll("Codec[Int]", CodecTests[Int].codec)
  checkAll("Codec[Long]", CodecTests[Long].codec)
}

class StdLibCodecSuite extends CirceSuite {
  checkAll("Codec[String]", CodecTests[String].codec)
  checkAll("Codec[BigInt]", CodecTests[BigInt].codec)
  checkAll("Codec[BigDecimal]", CodecTests[BigDecimal].codec)
  checkAll("Codec[UUID]", CodecTests[UUID].codec)
  checkAll("Codec[Option[Int]]", CodecTests[Option[Int]].codec)
  checkAll("Codec[List[Int]]", CodecTests[List[Int]].codec)
  checkAll("Codec[Map[String, Int]]", CodecTests[Map[String, Int]].codec)
  checkAll("Codec[Set[Int]]", CodecTests[Set[Int]].codec)

  test("Tuples should be encoded as JSON arrays") {
    check { (t: (Int, String, Char)) =>
      val json = Encoder[(Int, String, Char)].apply(t)
      val target = Json.array(Json.int(t._1), Json.string(t._2), Encoder[Char].apply(t._3))

      json === target && json.as[(Int, String, Char)] === Xor.right(t)
    }
  }

  test("Decoding a JSON array without enough elements into a tuple should fail") {
    check { (i: Int, s: String) =>
      Json.array(Json.int(i), Json.string(s)).as[(Int, String, Double)].isLeft
    }
  }

  test("Decoding a JSON array with too many elements into a tuple should fail") {
    check { (i: Int, s: String, d: Double) =>
      Json.array(Json.int(i), Json.string(s), Json.numberOrNull(d)).as[(Int, String)].isLeft
    }
  }

  test("Decoding a JSON array with many elements into a sequence should not stack overflow") {
    val size = 10000
    val jsonArr = Json.array(Seq.fill(size)(Json.int(1)): _*)

    val maybeList = jsonArr.as[List[Int]]
    assert(maybeList.isRight)

    val list = maybeList.getOrElse(???)
    assert(list.length == size)
    assert(list.forall(_ == 1))
  }

  test("Decoding a JSON array should stop after first failure") {
    case class Bomb(i: Int)

    object Bomb {
      implicit val decodeBomb: Decoder[Bomb] = Decoder[Int].map {
        case 0 => throw new Exception("You shouldn't have tried to decode this")
        case i => Bomb(i)
      }
    }

    val jsonArr = Json.array(Json.int(1), Json.string("foo"), Json.int(0))
    val result = jsonArr.as[List[Bomb]]

    assert(result.isLeft)
  }
}

class CatsCodecSuite extends CirceSuite {
  checkAll("Codec[NonEmptyList[Int]]", CodecTests[NonEmptyList[Int]].codec)
  checkAll("Codec[NonEmptyVector[Int]]", CodecTests[NonEmptyVector[Int]].codec)
  checkAll("Codec[NonEmptyStream[Int]]", CodecTests[NonEmptyStream[Int]].codec)
}

class CirceCodecSuite extends CirceSuite {
  checkAll("Codec[Json]", CodecTests[Json].codec)
  checkAll("Codec[JsonObject]", CodecTests[JsonObject].codec)
  checkAll("Codec[JsonNumber]", CodecTests[JsonNumber].codec)
}

class DisjunctionCodecSuite extends CirceSuite {
  import disjunctionCodecs._

  checkAll("Codec[Xor[Int, String]]", CodecTests[Xor[Int, String]].codec)
  checkAll("Codec[Either[Int, String]]", CodecTests[Either[Int, String]].codec)
  checkAll("Codec[Validated[String, Int]]", CodecTests[Validated[String, Int]].codec)
}

class DecodingFailureSuite extends CirceSuite {
  val n = Json.int(10)
  val b = Json.True
  val s = Json.string("foo")
  val l = Json.array(s)
  val o = Json.obj("foo" -> n)

  val nd = Decoder[Int]
  val bd = Decoder[Boolean]
  val sd = Decoder[String]
  val ld = Decoder[List[String]]
  val od = Decoder[Map[String, Int]]

  test("Decoding a JSON number as anything else should fail") {
    assert(List(bd, sd, ld, od).forall(d => d.decodeJson(n).isLeft))
  }

  test("Decoding a JSON boolean as anything else should fail") {
    assert(List(nd, sd, ld, od).forall(d => d.decodeJson(b).isLeft))
  }

  test("Decoding a JSON string as anything else should fail") {
    assert(List(nd, bd, ld, od).forall(d => d.decodeJson(s).isLeft))
  }

  test("Decoding a JSON array as anything else should fail") {
    assert(List(nd, bd, sd, od).forall(d => d.decodeJson(l).isLeft))
  }

  test("Decoding a JSON object as anything else should fail") {
    assert(List(nd, bd, sd, ld).forall(d => d.decodeJson(o).isLeft))
  }
}
