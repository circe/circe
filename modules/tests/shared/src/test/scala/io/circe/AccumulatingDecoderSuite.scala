package io.circe

import cats.data.NonEmptyList
import cats.instances.all._
import cats.laws.discipline.arbitrary._
import cats.syntax.eq._
import io.circe.syntax._
import io.circe.tests.CirceMunitSuite
import org.scalacheck.Prop.forAll

class AccumulatingDecoderSpec extends CirceMunitSuite {
  private case class BadSample(a: Int, b: Boolean, c: Int)

  private object BadSample {
    implicit val decodeBadSample: Decoder[BadSample] = Decoder.forProduct3("a", "b", "c")(BadSample.apply)
    implicit val encodeBadSample: Encoder[BadSample] = Encoder.forProduct3("a", "b", "c") {
      case BadSample(a, b, c) => (a, b, c)
    }
  }

  private case class Sample(a: String, b: String, c: String)

  private object Sample {
    implicit val decodeSample: Decoder[Sample] = Decoder.forProduct3("a", "b", "c")(Sample.apply)
    implicit val encodeSample: Encoder[Sample] = Encoder.forProduct3("a", "b", "c") {
      case Sample(a, b, c) => (a, b, c)
    }
  }

  property("return as many errors as invalid elements in a list") {
    forAll { (xs: List[Either[Int, String]]) =>
      val json = xs.map(_.fold(Json.fromInt, Json.fromString)).asJson
      val decoded = Decoder[List[String]].decodeAccumulating(json.hcursor)
      val intElems = xs.collect { case Left(elem) => elem }

      assert(decoded.fold(_.tail.size + 1, _ => 0) === intElems.size)
    }
  }

  property("return expected failures in a list") {
    forAll { (xs: List[Either[Int, String]]) =>
      val cursor = xs.map(_.fold(Json.fromInt, Json.fromString)).asJson.hcursor
      val invalidElems = xs.collect { case Left(e) => Some(e.asJson) }
      val errors = Decoder[List[String]].decodeAccumulating(cursor).fold(_.toList, _ => Nil)

      assert(errors.map(df => cursor.replay(df.history).focus) === invalidElems)
    }
  }

  property("return expected failures in a map") {
    forAll { (xs: Map[String, Either[Int, String]]) =>
      val cursor = xs.map { case (k, v) => (k, v.fold(Json.fromInt, Json.fromString)) }.asJson.hcursor
      val invalidElems = xs.values.collect { case Left(e) => e.asJson }.toSet
      val errors = Decoder[Map[String, String]].decodeAccumulating(cursor).fold(_.toList, _ => Nil)

      assert(errors.flatMap(df => cursor.replay(df.history).focus).toSet === invalidElems)
    }
  }

  property("return expected failures in a set") {
    forAll { (xs: Set[Either[Int, String]]) =>
      val cursor = xs.map(_.fold(Json.fromInt, Json.fromString)).asJson.hcursor
      val invalidElems = xs.collect { case Left(e) => Some(e.asJson): Option[Json] }
      val errors = Decoder[Set[String]].decodeAccumulating(cursor).fold(_.toList, _ => Nil)

      assert(errors.map(df => cursor.replay(df.history).focus).toSet === invalidElems)
    }
  }

  property("return expected failures in a non-empty list") {
    forAll { (xs: NonEmptyList[Either[Int, String]]) =>
      val cursor = xs.map(_.fold(Json.fromInt, Json.fromString)).asJson.hcursor
      val invalidElems = xs.toList.collect { case Left(e) => Some(e.asJson) }
      val errors = Decoder[NonEmptyList[String]].decodeAccumulating(cursor).fold(_.toList, _ => Nil)

      assert(errors.map(df => cursor.replay(df.history).focus) === invalidElems)
    }
  }

  property("return expected failures in a tuple") {
    forAll { (xs: (Either[Int, String], Either[Int, String], Either[Int, String])) =>
      val cursor = (
        xs._1.fold(Json.fromInt, Json.fromString),
        xs._2.fold(Json.fromInt, Json.fromString),
        xs._3.fold(Json.fromInt, Json.fromString)
      ).asJson.hcursor

      val invalidElems = xs.productIterator.toList.collect {
        case Left(e: Int) => Some(e.asJson)
      }

      val errors = Decoder[(String, String, String)].decodeAccumulating(cursor).fold(_.toList, _ => Nil)

      assertEquals(errors.map(df => cursor.replay(df.history).focus), invalidElems)
    }
  }

  property("return expected failures in a case class") {
    forAll { (a: Int, b: Boolean, c: Int) =>
      val cursor = BadSample(a, b, c).asJson.hcursor
      val invalidElems = List(Some(a.asJson), Some(b.asJson), Some(c.asJson))
      val errors = Decoder[Sample].decodeAccumulating(cursor).fold(_.toList, _ => Nil)

      assert(errors.map(df => cursor.replay(df.history).focus) === invalidElems)
    }
  }

  property("return expected failures combined with validation errors") {
    forAll { (a: Int, b: Boolean, c: Int) =>
      val cursor = BadSample(a, b, c).asJson.hcursor
      val invalidElems = List(Some(a.asJson), Some(b.asJson), Some(c.asJson))
      val result = Decoder[Sample].validate(_ => List("problem")).decodeAccumulating(cursor)

      result.fold(_.toList, _ => Nil) match {
        case validationError :: errors =>
          assert(validationError === DecodingFailure("problem", List.empty))
          assert(errors.map(df => cursor.replay(df.history).focus) === invalidElems)
        case _ => ()
      }
    }
  }
}
