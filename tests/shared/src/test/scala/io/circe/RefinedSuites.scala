package io.circe

import algebra.Eq
import io.circe.tests.{ CodecTests, CirceSuite }
import io.circe.refined._
import io.circe.syntax._

import eu.timepit.refined.{ refineMV, refineV }
import eu.timepit.refined.api.{ Refined, RefType }
import eu.timepit.refined.string.StartsWith
import eu.timepit.refined.numeric.{ Positive, Greater }
import eu.timepit.refined.collection.{ NonEmpty, Size }
import eu.timepit.refined.scalacheck.numericArbitrary.greaterArbitrary
import eu.timepit.refined.scalacheck.stringArbitrary.startsWithArbitrary
import shapeless.{ Nat, Witness => W }

import org.scalacheck.{ Gen, Arbitrary }

class RefinedSuite extends CirceSuite {

  implicit def refinedEq[T, P, F[_, _]](implicit refType: RefType[F]): Eq[F[T, P]] =
    Eq.fromUniversalEquals

  checkAll(
    """ Codec[Int Refined Greater[W.`2`.T]] """,
    CodecTests[Int Refined Greater[W.`2`.T]].codec
  )
  checkAll(
    """ Codec[String Refined StartsWith[W.`"a"`.T]] """,
    CodecTests[String Refined StartsWith[W.`"a"`.T]].codec
  )

  test("Refined instances should encode as the underlying type") {
    val n = refineMV[Greater[W.`2`.T]](5)
    n.asJson shouldBe 5.asJson

    val list = List(1, 2, 3, 4)
    val refinedList = refineV[Size[Greater[Nat._3]]](list)
    refinedList.right.map(_.asJson) shouldBe Right(list.asJson)
  }

  test("Refined instances should refuse to decode wrong values") {
    Decoder[Int Refined Greater[W.`2`.T]].decodeJson(3.asJson).isRight shouldBe true
    Decoder[Int Refined Greater[W.`2`.T]].decodeJson(1.asJson).isLeft shouldBe true

    Decoder[String Refined StartsWith[W.`"a"`.T]].decodeJson("ab".asJson).isRight shouldBe true
    Decoder[String Refined StartsWith[W.`"a"`.T]].decodeJson("ba".asJson).isLeft shouldBe true
  }

}

class RefinedFieldsSuite extends CirceSuite {
  import io.circe.generic.auto._

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
        RefType[Refined].unsafeWrap(sh + st),
        RefType[Refined].unsafeWrap(l0 :: l1 :: l2 :: lr)
      )
    )
  }

  checkAll("Codec[RefinedFields]", CodecTests[RefinedFields].codec)

  test("Refined fields should be encoded as simple fields") {
    val json = Encoder[RefinedFields].apply(RefinedFields(
      refineMV(3),
      refineMV("ab"),
      refineV[Size[Greater[Nat._2]]](List(1, 2, 3, 4)).right.get
    ))
    val expectedJson = Json.obj(
      "i" -> 3.asJson,
      "s" -> "ab".asJson,
      "l" -> List(1, 2, 3, 4).asJson
    )

    json shouldBe expectedJson
  }

}
