package io.circe

import cats.data.NonEmptyList
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.tests.CirceSuite

class AccumulatingDecoderSuite extends CirceSuite {

  test("Accumulating decoder returns as many errors as invalid elements in a list") {
    check { (xs: List[Either[String, Int]]) =>
      val json = xs.map(_.fold(Json.string, Json.int)).asJson
      val decoded = Decoder[List[String]].accumulating(json.hcursor)
      val intElems = xs.collect { case Right(elem) => elem }

      decoded.fold(_.tail.size + 1, _ => 0) === intElems.size
    }
  }

  test("Accumulating decoder returns as many errors as invalid elements in a map") {
    check { (xs: Map[String, Either[String, Int]]) =>
      val json = xs.map { case (k, v) => (k, v.fold(Json.string, Json.int)) }.asJson
      val decoded = Decoder[Map[String, String]].accumulating(json.hcursor)
      val intElems = xs.values.collect { case Right(elem) => elem }

      decoded.fold(_.tail.size + 1, _ => 0) === intElems.size
    }
  }

  test("Accumulating decoder returns as many errors as invalid elements in a set") {
    check { (xs: Set[Either[String, Int]]) =>
      val json = xs.map(_.fold(Json.string, Json.int)).asJson
      val decoded = Decoder[Set[String]].accumulating(json.hcursor)
      val intElems = xs.collect { case Right(elem) => elem }

      decoded.fold(_.tail.size + 1, _ => 0) === intElems.size
    }
  }

  test("Accumulating decoder returns as many errors as invalid elements in a tuple") {
    check { (xs: (Either[String, Int], Either[String, Int], Either[String, Int])) =>
      val json = (
        xs._1.fold(Json.string, Json.int),
        xs._2.fold(Json.string, Json.int),
        xs._3.fold(Json.string, Json.int)
      ).asJson
      val decoded = Decoder[(String, String, String)].accumulating(json.hcursor)
      val intElems = List(xs._1, xs._2, xs._3).collect { case Right(elem) => elem }

      decoded.fold(_.tail.size + 1, _ => 0) === intElems.size
    }
  }

  test("Accumulating decoder returns as many errors as invalid elements in a non-empty list") {
    check { (nel: NonEmptyList[Either[String, Int]]) =>
      val json = nel.map(_.fold(Json.string, Json.int)).asJson
      val decoded = Decoder[NonEmptyList[String]].accumulating(json.hcursor)
      val intElems = nel.toList.collect { case Right(elem) => elem }

      decoded.fold(_.tail.size + 1, _ => 0) === intElems.size
    }
  }

  test("Accumulating decoder returns as many errors as invalid elements in a case class") {
    case class Sample(a: Int, b: Boolean, c: Int)
    case class BadSample(a: String, b: String, c: String)

    check { (a: Int, b: Boolean, c: Int) =>
      val json = new Sample(a, b, c).asJson
      val decoded = Decoder[BadSample].accumulating(json.hcursor)

      decoded.fold(_.tail.size + 1, _ => 0) === 3
    }
  }

  //TODO: add tests replaying cursor history to ensure it is correct (next push)
  ignore("Accumulating decoder error messages name all the invalid elements in a JSON array") {
    fail
  }
}
