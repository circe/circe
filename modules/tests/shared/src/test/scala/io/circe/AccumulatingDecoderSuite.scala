package io.circe

import cats.data.NonEmptyList
import cats.laws.discipline.{ ApplicativeErrorTests, SemigroupKTests }
import cats.laws.discipline.arbitrary._
import io.circe.ast.{ Json, JsonObject }
import io.circe.generic.semiauto._
import io.circe.syntax._
import io.circe.tests.CirceSuite
import io.circe.tests.examples.Qux

class AccumulatingDecoderSpec extends CirceSuite {
  checkLaws(
    "AccumulatingDecoder[Int]",
    ApplicativeErrorTests[AccumulatingDecoder, NonEmptyList[DecodingFailure]].applicativeError[Int, Int, Int]
  )

  checkLaws("AccumulatingDecoder[Int]", SemigroupKTests[AccumulatingDecoder].semigroupK[Int])

  private case class BadSample(a: Int, b: Boolean, c: Int)

  private object BadSample {
    implicit val decodeBadSample: Decoder[BadSample] = deriveDecoder
    implicit val encodeBadSample: Encoder[BadSample] = deriveEncoder
  }

  private case class Sample(a: String, b: String, c: String)

  private object Sample {
    implicit val decodeSample: Decoder[Sample] = deriveDecoder
    implicit val encodeSample: Encoder[Sample] = deriveEncoder
  }

  "accumulating" should "return as many errors as invalid elements in a list" in {
    forAll { (xs: List[Either[Int, String]]) =>
      val json = xs.map(_.fold(Json.fromInt, Json.fromString)).asJson
      val decoded = Decoder[List[String]].accumulating(HCursor.fromJson(json))
      val intElems = xs.collect { case Left(elem) => elem }

      assert(decoded.fold(_.tail.size + 1, _ => 0) === intElems.size)
    }
  }

  it should "return expected failures in a list" in {
    forAll { (xs: List[Either[Int, String]]) =>
      val cursor = HCursor.fromJson(xs.map(_.fold(Json.fromInt, Json.fromString)).asJson)
      val invalidElems = xs.collect { case Left(e) => Some(e.asJson) }
      val errors = Decoder[List[String]].accumulating(cursor).fold(_.toList, _ => Nil)

      assert(errors.map(df => cursor.replay(df.history).focus) === invalidElems)
    }
  }

  it should "return expected failures in a map" in {
    forAll { (xs: Map[String, Either[Int, String]]) =>
      val cursor = HCursor.fromJson(xs.map { case (k, v) => (k, v.fold(Json.fromInt, Json.fromString)) }.asJson)
      val invalidElems = xs.values.collect { case Left(e) => e.asJson }.toSet
      val errors = Decoder[Map[String, String]].accumulating(cursor).fold(_.toList, _ => Nil)

      assert(errors.flatMap(df => cursor.replay(df.history).focus).toSet === invalidElems)
    }
  }

  it should "return expected failures in a set" in {
    forAll { (xs: Set[Either[Int, String]]) =>
      val cursor = HCursor.fromJson(xs.map(_.fold(Json.fromInt, Json.fromString)).asJson)
      val invalidElems = xs.collect { case Left(e) => Some(e.asJson): Option[Json] }
      val errors = Decoder[Set[String]].accumulating(cursor).fold(_.toList, _ => Nil)

      assert(errors.map(df => cursor.replay(df.history).focus).toSet === invalidElems)
    }
  }

  it should "return expected failures in a non-empty list" in {
    forAll { (xs: NonEmptyList[Either[Int, String]]) =>
      val cursor = HCursor.fromJson(xs.map(_.fold(Json.fromInt, Json.fromString)).asJson)
      val invalidElems = xs.toList.collect { case Left(e) => Some(e.asJson) }
      val errors = Decoder[NonEmptyList[String]].accumulating(cursor).fold(_.toList, _ => Nil)

      assert(errors.map(df => cursor.replay(df.history).focus) === invalidElems)
    }
  }

  it should "return expected failures in a tuple" in {
    forAll { (xs: (Either[Int, String], Either[Int, String], Either[Int, String])) =>
      val cursor = HCursor.fromJson(
        (
          xs._1.fold(Json.fromInt, Json.fromString),
          xs._2.fold(Json.fromInt, Json.fromString),
          xs._3.fold(Json.fromInt, Json.fromString)
        ).asJson
      )

      val invalidElems = xs.productIterator.toList.collect {
        case Left(e: Int) => Some(e.asJson)
      }

      val errors = Decoder[(String, String, String)].accumulating(cursor).fold(_.toList, _ => Nil)

      assert(errors.map(df => cursor.replay(df.history).focus) === invalidElems)
    }
  }

  it should "return expected failures in a case class" in {
    forAll { (a: Int, b: Boolean, c: Int) =>
      val cursor = HCursor.fromJson(BadSample(a, b, c).asJson)
      val invalidElems = List(Some(a.asJson), Some(b.asJson), Some(c.asJson))
      val errors = Decoder[Sample].accumulating(cursor).fold(_.toList, _ => Nil)

      assert(errors.map(df => cursor.replay(df.history).focus) === invalidElems)
    }
  }

  it should "return as many errors as invalid elements in a partial case class" in {
    val decoded = deriveFor[Int => Qux[String]].incomplete.accumulating(
      HCursor.fromJson(Json.fromJsonObject(JsonObject.empty))
    )

    assert(decoded.fold(_.tail.size + 1, _ => 0) === 2)
  }
}
