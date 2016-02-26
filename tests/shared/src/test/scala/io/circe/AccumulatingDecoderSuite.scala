package io.circe

import cats.data.NonEmptyList
import io.circe.generic.semiauto._
import io.circe.syntax._
import io.circe.tests.CirceSuite

class AccumulatingDecoderSuite extends CirceSuite {

  test("Accumulating decoder returns as many errors as invalid elements in a list") {
    check { (xs: List[Either[Int, String]]) =>
      val json = xs.map(_.fold(Json.fromInt, Json.fromString)).asJson
      val decoded = Decoder[List[String]].accumulating(json.hcursor)
      val intElems = xs.collect { case Left(elem) => elem }

      decoded.fold(_.tail.size + 1, _ => 0) === intElems.size
    }
  }

  test("replaying accumulating decoder history returns expected failures in a list") {
    check { (xs: List[Either[Int, String]]) =>
      val json = xs.map(_.fold(Json.fromInt, Json.fromString)).asJson
      val cursor = Cursor(json).hcursor

      val invalidElems = xs.collect { case Left(e) => Option(e.asJson) }
      Decoder[List[String]].accumulating(json.hcursor)
        .fold(error => error.toList, _ => List[DecodingFailure]())
        .map(df => cursor.replay(df.history).focus) === invalidElems
    }
  }

  test("Accumulating decoder returns as many errors as invalid elements in a map") {
    check { (xs: Map[String, Either[Int, String]]) =>
      val json = xs.map { case (k, v) => (k, v.fold(Json.fromInt, Json.fromString)) }.asJson
      val decoded = Decoder[Map[String, String]].accumulating(json.hcursor)
      val intElems = xs.values.collect { case Left(elem) => elem }

      decoded.fold(_.tail.size + 1, _ => 0) === intElems.size
    }
  }

  test("replaying accumulating decoder history returns expected failures in a map") {
    check { (xs: Map[String, Either[Int, String]]) =>
      val json = xs.map { case (k, v) => (k, v.fold(Json.fromInt, Json.fromString)) }.asJson
      val cursor = Cursor(json).hcursor

      val invalidElems = xs.values.collect { case Left(e) => Option(e.asJson) }.toSet

      Decoder[Map[String, String]].accumulating(json.hcursor)
        .fold(error => error.toList, _ => List[DecodingFailure]())
        .map(df => cursor.replay(df.history).focus).toSet === invalidElems
    }
  }

  test("Accumulating decoder returns as many errors as invalid elements in a set") {
    check { (xs: Set[Either[Int, String]]) =>
      val json = xs.map(_.fold(Json.fromInt, Json.fromString)).asJson
      val decoded = Decoder[Set[String]].accumulating(json.hcursor)
      val intElems = xs.collect { case Left(elem) => elem }

      decoded.fold(_.tail.size + 1, _ => 0) === intElems.size
    }
  }

  test("replaying accumulating decoder history returns expected failures in a set") {
    check { (xs: Set[Either[Int, String]]) =>
      val json = xs.map(_.fold(Json.fromInt, Json.fromString)).asJson
      val cursor = Cursor(json).hcursor

      val invalidElems = xs.collect { case Left(e) => Option(e.asJson) }

      Decoder[Set[String]].accumulating(json.hcursor)
        .fold(error => error.toList, _ => List[DecodingFailure]())
        .map(df => cursor.replay(df.history).focus).toSet === invalidElems
    }
  }

  test("Accumulating decoder returns as many errors as invalid elements in a tuple") {
    check { (xs: (Either[Int, String], Either[Int, String], Either[Int, String])) =>
      val json = (
        xs._1.fold(Json.fromInt, Json.fromString),
        xs._2.fold(Json.fromInt, Json.fromString),
        xs._3.fold(Json.fromInt, Json.fromString)
        ).asJson
      val decoded = Decoder[(String, String, String)].accumulating(json.hcursor)
      val intElems = List(xs._1, xs._2, xs._3).collect { case Left(elem) => elem }

      decoded.fold(_.tail.size + 1, _ => 0) === intElems.size
    }
  }

  test("replaying accumulating decoder history returns expected failures in a tuple") {
    check { (xs: (Either[Int, String], Either[Int, String], Either[Int, String])) =>
      val json = (
        xs._1.fold(Json.fromInt, Json.fromString),
        xs._2.fold(Json.fromInt, Json.fromString),
        xs._3.fold(Json.fromInt, Json.fromString)
        ).asJson
      val cursor = Cursor(json).hcursor

      val invalidElems = xs.productIterator
        .map(_.asInstanceOf[Either[Int, String]])
        .collect { case Left(e) => Option(e.asJson) }
        .toList

      Decoder[(String, String, String)].accumulating(json.hcursor)
        .fold(error => error.toList, _ => List[DecodingFailure]())
        .map(df => cursor.replay(df.history).focus) === invalidElems
    }
  }

  test("Accumulating decoder returns as many errors as invalid elements in a non-empty list") {
    check { (nel: NonEmptyList[Either[Int, String]]) =>
      val json = nel.map(_.fold(Json.fromInt, Json.fromString)).asJson
      val decoded = Decoder[NonEmptyList[String]].accumulating(json.hcursor)
      val intElems = nel.toList.collect { case Left(elem) => elem }

      decoded.fold(_.tail.size + 1, _ => 0) === intElems.size
    }
  }

  test("replaying accumulating decoder history returns expected failures in a non-empty list") {
    check { (nel: NonEmptyList[Either[Int, String]]) =>
      val json = nel.map(_.fold(Json.fromInt, Json.fromString)).asJson
      val cursor = Cursor(json).hcursor

      val invalidElems = nel.toList.collect { case Left(e) => Option(e.asJson) }
      Decoder[NonEmptyList[String]].accumulating(json.hcursor)
        .fold(error => error.toList, _ => List[DecodingFailure]())
        .map(df => cursor.replay(df.history).focus) === invalidElems
    }
  }

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

  test("Accumulating decoder returns as many errors as invalid elements in a case class") {
    check { (a: Int, b: Boolean, c: Int) =>
      val json = BadSample(a, b, c).asJson
      val decoded = Decoder[Sample].accumulating(json.hcursor)

      decoded.fold(_.tail.size + 1, _ => 0) === 3
    }
  }

  test("replaying accumulating decoder history returns expected failures in a case class") {
    check { (a: Int, b: Boolean, c: Int) =>
      val json = BadSample(a, b, c).asJson
      val cursor = Cursor(json).hcursor

      val invalidElems = List(Some(a.asJson), Some(b.asJson), Some(c.asJson))

      Decoder[Sample].accumulating(json.hcursor)
        .fold(error => error.toList, _ => List[DecodingFailure]())
        .map(df => cursor.replay(df.history).focus) === invalidElems
    }
  }
}
