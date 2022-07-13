package io.circe.refined

import cats.kernel.Eq
import cats.kernel.instances.all._
import cats.syntax.eq._
import eu.timepit.refined.{ refineMV, refineV }
import eu.timepit.refined.api.{ RefType, Refined }
import eu.timepit.refined.string.StartsWith
import eu.timepit.refined.numeric.{ Greater, Positive }
import eu.timepit.refined.collection.{ NonEmpty, Size }
import eu.timepit.refined.scalacheck.numeric.greaterArbitrary
import eu.timepit.refined.scalacheck.string.startsWithArbitrary
import io.circe.{ Decoder, Encoder, Json, KeyDecoder, KeyEncoder }
import io.circe.testing.CodecTests
import io.circe.tests.CirceMunitSuite
import io.circe.syntax._
import org.scalacheck.{ Arbitrary, Gen }
import shapeless.{ Nat, Witness => W }

class RefinedSuite extends CirceMunitSuite {
  implicit def refinedEq[T, P, F[_, _]](implicit refType: RefType[F]): Eq[F[T, P]] = Eq.fromUniversalEquals

  type Gt2 = Greater[W.`2`.T]

  checkAll(
    "Codec[Int Refined Greater[W.`2`.T]]",
    CodecTests[Int Refined Gt2].codec
  )

  checkAll(
    """Codec[String Refined StartsWith[W.`"a"`.T]]""",
    CodecTests[String Refined StartsWith[W.`"a"`.T]].codec
  )

  test("A refined encoder should encode as the underlying type") {
    val n = refineMV[Gt2](5)
    assert(n.asJson === 5.asJson)

    val list = List(1, 2, 3, 4)
    val refinedList = refineV[Size[Greater[Nat._3]]](list)
    val expected: Either[String, Json] = Right(list.asJson)

    assert(expected === refinedList.map(_.asJson))
  }

  test("A refined decoder should refuse to decode wrong values") {
    assert(Decoder[Int Refined Gt2].decodeJson(3.asJson).isRight)
    assert(Decoder[Int Refined Gt2].decodeJson(1.asJson).isLeft)

    assert(Decoder[String Refined StartsWith[W.`"a"`.T]].decodeJson("ab".asJson).isRight)
    assert(Decoder[String Refined StartsWith[W.`"a"`.T]].decodeJson("ba".asJson).isLeft)
  }

  test("A refined key encoder should encode as string") {
    val n = refineMV[Gt2](5)
    val s = refineMV[NonEmpty]("a")

    assert(KeyEncoder[Int Refined Gt2].apply(n) === "5")
    assert(KeyEncoder[String Refined NonEmpty].apply(s) === "a")
  }

  test("A refined key decoder should refuse to decode wrong values") {
    assert(KeyDecoder[Int Refined Gt2].apply("3").isDefined)
    assert(KeyDecoder[Int Refined Gt2].apply("1").isEmpty)

    assert(KeyDecoder[String Refined NonEmpty].apply("a").isDefined)
    assert(KeyDecoder[String Refined NonEmpty].apply("").isEmpty)
  }
}

object RefinedFieldsSuite {
  case class RefinedFields(
    i: Int Refined Positive,
    s: String Refined NonEmpty,
    l: List[Int] Refined Size[Greater[Nat._2]]
  )

  object RefinedFields {
    implicit val eq: Eq[RefinedFields] = Eq.fromUniversalEquals
    implicit val arbitrary: Arbitrary[RefinedFields] = Arbitrary(
      for {
        i <- Gen.choose(0, Int.MaxValue)
        sh <- Arbitrary.arbitrary[Char]
        st <- Arbitrary.arbitrary[String]
        (l0, l1, l2) <- Arbitrary.arbitrary[(Int, Int, Int)]
        lr <- Arbitrary.arbitrary[List[Int]]
      } yield RefinedFields(
        RefType[Refined].unsafeWrap(i),
        RefType[Refined].unsafeWrap(s"$sh$st"),
        RefType[Refined].unsafeWrap(l0 :: l1 :: l2 :: lr)
      )
    )

    implicit val decodeRefinedFields: Decoder[RefinedFields] = Decoder.forProduct3("i", "s", "l")(RefinedFields.apply)
    implicit val encodeRefinedFields: Encoder[RefinedFields] = Encoder.forProduct3("i", "s", "l") {
      case RefinedFields(i, s, l) => (i, s, l)
    }
  }
}

class RefinedFieldsSuite extends CirceMunitSuite {
  import RefinedFieldsSuite._

  checkAll("Codec[RefinedFields]", CodecTests[RefinedFields].codec)

  test("Refined fields should be encoded as simple fields") {
    val json = Encoder[RefinedFields].apply(
      RefinedFields(
        refineMV(3),
        refineMV("ab"),
        refineV[Size[Greater[Nat._2]]](List(1, 2, 3, 4)).toOption.get
      )
    )

    val expectedJson = Json.obj(
      "i" -> 3.asJson,
      "s" -> "ab".asJson,
      "l" -> List(1, 2, 3, 4).asJson
    )

    assert(json === expectedJson)
  }
}

class RefinedKeysSuite extends CirceMunitSuite {
  test("Refined keys should be encoded as Map keys") {
    val example: Map[String Refined NonEmpty, Int] = Map(refineMV[NonEmpty]("a") -> 1, refineMV[NonEmpty]("b") -> 2)

    val expectedJson = Json.obj("a" -> 1.asJson, "b" -> 2.asJson)

    assert(example.asJson === expectedJson)
  }

  test("Refined Keys should decode when valid") {
    val json = Json.obj("a" -> 1.asJson, "b" -> 2.asJson)
    val expected: Map[String Refined NonEmpty, Int] = Map(refineMV[NonEmpty]("a") -> 1, refineMV[NonEmpty]("b") -> 2)
    assert(json.as[Map[String Refined NonEmpty, Int]] === Right(expected))
  }

  test("Refined Keys should not decode when invalid") {
    val json = Json.obj("a" -> 1.asJson, "" -> 2.asJson)
    assert(json.as[Map[String Refined NonEmpty, Int]].isLeft)
  }
}
