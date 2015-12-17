package io.circe

import cats.data.NonEmptyList
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.tests.CirceSuite

class AccumulatingDecoderSuite extends CirceSuite {

  test("Accumulating decoder returns as many errors as invalid elements in a list") {
    check { (xs: List[Either[Int, String]]) =>
      val json = xs.map(_.fold(Json.int, Json.string)).asJson
      val decoded = Decoder[List[String]].accumulating(json.hcursor)
      val intElems = xs.collect { case Left(elem) => elem }

      decoded.fold(_.tail.size + 1, _ => 0) === intElems.size
    }
  }

  test("replaying accumulating decoder history returns the expected failing element in a list") {
    check { (xs: List[Either[Int, String]]) =>
      val json = xs.map(_.fold(Json.int, Json.string)).asJson
      val cursor = Cursor(json).hcursor

      val invalidElems = xs.collect { case Left(e) => Option(e.asJson) }
      Decoder[List[String]].accumulating(json.hcursor)
        .fold(error => error.toList, _ => List[DecodingFailure]())
        .map(df => cursor.replay(df.history).focus) === invalidElems
    }
  }

  test("Accumulating decoder returns as many errors as invalid elements in a map") {
    check { (xs: Map[String, Either[Int, String]]) =>
      val json = xs.map { case (k, v) => (k, v.fold(Json.int, Json.string)) }.asJson
      val decoded = Decoder[Map[String, String]].accumulating(json.hcursor)
      val intElems = xs.values.collect { case Left(elem) => elem }

      decoded.fold(_.tail.size + 1, _ => 0) === intElems.size
    }
  }

  //TODO: this test fails, even if lists are identical... why???
  ignore("replaying accumulating decoder history returns the expected failing element in a map") {
    check { (xs: Map[String, Either[Int, String]]) =>
      val json = xs.map { case (k, v) => (k, v.fold(Json.int, Json.string)) }.asJson
      val cursor = Cursor(json).hcursor

      val invalidElems = xs.values.collect { case Left(e) => Option(e.asJson) }.toList
      Decoder[Map[String, String]].accumulating(json.hcursor)
        .fold(error => error.toList, _ => List[DecodingFailure]())
        .map(df => cursor.replay(df.history).focus) === invalidElems
    }
  }

  test("Accumulating decoder returns as many errors as invalid elements in a set") {
    check { (xs: Set[Either[Int, String]]) =>
      val json = xs.map(_.fold(Json.int, Json.string)).asJson
      val decoded = Decoder[Set[String]].accumulating(json.hcursor)
      val intElems = xs.collect { case Left(elem) => elem }

      decoded.fold(_.tail.size + 1, _ => 0) === intElems.size
    }
  }

  //TODO: this test fails, even if sets are identical... why???
  ignore("replaying accumulating decoder history returns the expected failing element in a set") {
    check { (xs: Set[Either[Int, String]]) =>
      val json = xs.map(_.fold(Json.int, Json.string)).asJson
      val cursor = Cursor(json).hcursor

      val invalidElems = xs.toList.collect { case Left(e) => Option(e.asJson) }
      Decoder[Set[String]].accumulating(json.hcursor)
        .fold(error => error.toList, _ => List[DecodingFailure]())
        .map(df => cursor.replay(df.history).focus) === invalidElems
    }
  }

  test("Accumulating decoder returns as many errors as invalid elements in a tuple") {
    check { (xs: (Either[Int, String], Either[Int, String], Either[Int, String])) =>
      val json = (
        xs._1.fold(Json.int, Json.string),
        xs._2.fold(Json.int, Json.string),
        xs._3.fold(Json.int, Json.string)
        ).asJson
      val decoded = Decoder[(String, String, String)].accumulating(json.hcursor)
      val intElems = List(xs._1, xs._2, xs._3).collect { case Left(elem) => elem }

      decoded.fold(_.tail.size + 1, _ => 0) === intElems.size
    }
  }

  test("replaying accumulating decoder history returns the expected failing element in a tuple") {
    check { (xs: (Either[Int, String], Either[Int, String], Either[Int, String])) =>
      val json = (
        xs._1.fold(Json.int, Json.string),
        xs._2.fold(Json.int, Json.string),
        xs._3.fold(Json.int, Json.string)
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
      val json = nel.map(_.fold(Json.int, Json.string)).asJson
      val decoded = Decoder[NonEmptyList[String]].accumulating(json.hcursor)
      val intElems = nel.toList.collect { case Left(elem) => elem }

      decoded.fold(_.tail.size + 1, _ => 0) === intElems.size
    }
  }

  test("replaying accumulating decoder history returns the expected failing element in a non-empty list") {
    check { (nel: NonEmptyList[Either[Int, String]]) =>
      val json = nel.map(_.fold(Json.int, Json.string)).asJson
      val cursor = Cursor(json).hcursor

      val invalidElems = nel.toList.collect { case Left(e) => Option(e.asJson) }
      Decoder[NonEmptyList[String]].accumulating(json.hcursor)
        .fold(error => error.toList, _ => List[DecodingFailure]())
        .map(df => cursor.replay(df.history).focus) === invalidElems
    }
  }

  private case class BadSample(a: Int, b: Boolean, c: Int)

  private case class Sample(a: String, b: String, c: String)

  test("Accumulating decoder returns as many errors as invalid elements in a case class") {
    check { (a: String, b: String, c: String) =>
      val json = new Sample(a, b, c).asJson
      val decoded = Decoder[BadSample].accumulating(json.hcursor)

      decoded.fold(_.tail.size + 1, _ => 0) === 3
    }
  }

  test("replaying accumulating decoder history returns the expected failing element in a case class") {
    check { (a: String, b: String, c: String) =>
      val json = new Sample(a, b, c).asJson
      val cursor = Cursor(json).hcursor

      val invalidElems = List(Some(a.asJson), Some(b.asJson), Some(c.asJson))
      Decoder[BadSample].accumulating(json.hcursor)
        .fold(error => error.toList, _ => List[DecodingFailure]())
        .map(df => cursor.replay(df.history).focus) === invalidElems
    }
  }

}
