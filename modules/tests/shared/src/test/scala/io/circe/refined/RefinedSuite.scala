package io.circe.refined

import cats.Eq
import eu.timepit.refined.{ refineMV, refineV }
import eu.timepit.refined.api.{ Refined, RefType }
import eu.timepit.refined.string.StartsWith
import eu.timepit.refined.numeric.{ Positive, Greater }
import eu.timepit.refined.collection.{ NonEmpty, Size }
import eu.timepit.refined.scalacheck.numeric.greaterArbitraryWit
import eu.timepit.refined.scalacheck.string.startsWithArbitrary
import io.circe.{ Decoder, Encoder }
import io.circe.ast.Json
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import io.circe.syntax._
import org.scalacheck.{ Gen, Arbitrary }
import shapeless.{ Nat, Witness => W }

class RefinedSuite extends CirceSuite {
  implicit def refinedEq[T, P, F[_, _]](implicit refType: RefType[F]): Eq[F[T, P]] = Eq.fromUniversalEquals

  checkLaws(
    "Codec[Int Refined Greater[W.`2`.T]]",
    CodecTests[Int Refined Greater[W.`2`.T]].codec
  )

  checkLaws(
    """Codec[String Refined StartsWith[W.`"a"`.T]]""",
    CodecTests[String Refined StartsWith[W.`"a"`.T]].codec
  )

  "A refined encoder" should "encode as the underlying type" in {
    val n = refineMV[Greater[W.`2`.T]](5)
    assert(n.asJson === 5.asJson)

    val list = List(1, 2, 3, 4)
    val refinedList = refineV[Size[Greater[Nat._3]]](list)
    val expected: Either[String, Json] = Right(list.asJson)

    assert(expected === refinedList.right.map(_.asJson))
  }

  "A refined decoder" should "refuse to decode wrong values" in {
    assert(Decoder[Int Refined Greater[W.`2`.T]].decodeJson(3.asJson).isRight)
    assert(Decoder[Int Refined Greater[W.`2`.T]].decodeJson(1.asJson).isLeft)

    assert(Decoder[String Refined StartsWith[W.`"a"`.T]].decodeJson("ab".asJson).isRight)
    assert(Decoder[String Refined StartsWith[W.`"a"`.T]].decodeJson("ba".asJson).isLeft)
  }
}

class RefinedFieldsSuite extends CirceSuite {
  import io.circe.generic.semiauto._

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

    implicit val decodeRefinedFields: Decoder[RefinedFields] = deriveDecoder
    implicit val encodeRefinedFields: Encoder[RefinedFields] = deriveEncoder
  }

  checkLaws("Codec[RefinedFields]", CodecTests[RefinedFields].codec)

  "Refined fields" should "be encoded as simple fields" in {
    val json = Encoder[RefinedFields].apply(
      RefinedFields(
        refineMV(3),
        refineMV("ab"),
        refineV[Size[Greater[Nat._2]]](List(1, 2, 3, 4)).right.get
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
