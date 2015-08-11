package io.circe

import cats.data.{ NonEmptyList, Validated, Xor }
import cats.laws.discipline.arbitrary._
import io.circe.test.{ CodecTests, CirceSuite }

class AnyValCodecTests extends CirceSuite {
  checkAll("Codec[Unit]", CodecTests[Unit].codec)
  checkAll("Codec[Boolean]", CodecTests[Boolean].codec)
  checkAll("Codec[Char]", CodecTests[Char].codec)
  checkAll("Codec[Float]", CodecTests[Float].codec)
  checkAll("Codec[Double]", CodecTests[Double].codec)
  checkAll("Codec[Byte]", CodecTests[Byte].codec)
  checkAll("Codec[Short]", CodecTests[Short].codec)
  checkAll("Codec[Int]", CodecTests[Int].codec)
  checkAll("Codec[Long]", CodecTests[Long].codec)
}

class StdLibCodecTests extends CirceSuite {
  checkAll("Codec[String]", CodecTests[String].codec)
  checkAll("Codec[BigInt]", CodecTests[BigInt].codec)
  checkAll("Codec[BigDecimal]", CodecTests[BigDecimal].codec)
  checkAll("Codec[Option[Int]]", CodecTests[Option[Int]].codec)
  checkAll("Codec[List[Int]]", CodecTests[List[Int]].codec)
  checkAll("Codec[Map[String, Int]]", CodecTests[Map[String, Int]].codec)
  checkAll("Codec[Set[Int]]", CodecTests[Set[Int]].codec)
}

class CatsCodecTests extends CirceSuite {
  checkAll("Codec[NonEmptyList[Int]]", CodecTests[NonEmptyList[Int]].codec)
}

class CirceCodecTests extends CirceSuite {
  checkAll("Codec[Json]", CodecTests[Json].codec)
}

class DisjunctionCodecTests extends CirceSuite {
  import disjunctionCodecs._

  checkAll("Codec[Xor[Int, String]]", CodecTests[Xor[Int, String]].codec)
  checkAll("Codec[Either[Int, String]]", CodecTests[Either[Int, String]].codec)
  checkAll("Codec[Validated[String, Int]]", CodecTests[Validated[String, Int]].codec)
}

class DecodingFailureTests extends CirceSuite {
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
