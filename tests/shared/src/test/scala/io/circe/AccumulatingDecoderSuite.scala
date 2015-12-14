package io.circe

import cats.data.NonEmptyList
import io.circe.AccumulatingDecoder.Result
import io.circe.tests.CirceSuite
import io.circe.generic.auto._
import io.circe.syntax._


class AccumulatingDecoderSuite extends CirceSuite {

  test("Accumulating decoder returns as many errors as invalid elements in a List") {
    check { (xs: List[Either[String, Int]]) =>
      val json = xs.map(_.fold(Json.string, Json.int)).asJson
      val decoded: Result[List[Int]] = Decoder[List[Int]].accumulating(json.hcursor)
      val stringElems = xs.collect { case elem if elem.isLeft => elem.left.get }

      decoded.fold(nel => nel.tail.size + 1, _ => 0) === stringElems.size
    }
  }

  test("Accumulating decoder returns as many errors as invalid elements in a Map") {
    check { (xs: Map[String, Either[String, Int]]) =>
      val json = xs.map { case (k, v) => (k, v.fold(Json.string, Json.int)) }.asJson
      val decoded = Decoder[Map[String, Int]].accumulating(json.hcursor)
      val stringElems = xs.collect { case (k, elem) if elem.isLeft => elem.left.get }

      decoded.fold(nel => nel.tail.size + 1, _ => 0) === stringElems.size
    }
  }

  test("Accumulating decoder returns as many errors as invalid elements in a Set") {
    check { (xs: Set[Either[String, Int]]) =>
      val json = xs.map(_.fold(Json.string, Json.int)).asJson
      val decoded = Decoder[Set[Int]].accumulating(json.hcursor)
      val stringElems = xs.collect { case elem if elem.isLeft => elem.left.get }

      decoded.fold(nel => nel.tail.size + 1, _ => 0) === stringElems.size
    }
  }

  test("Accumulating decoder returns as many errors as invalid elements in a Tuple") {
    check { (xs: (Either[String, Int], Either[String, Int], Either[String, Int])) =>
      val json = (xs._1.fold(Json.string, Json.int),
        xs._2.fold(Json.string, Json.int),
        xs._3.fold(Json.string, Json.int)).asJson
      val decoded = Decoder[(Int, Int, Int)].accumulating(json.hcursor)
      val stringElems = xs.productIterator
        .map(_.asInstanceOf[Either[String, Int]])
        .collect { case elem if elem.isLeft => elem.left.get }

      decoded.fold(nel => nel.tail.size + 1, _ => 0) === stringElems.size
    }
  }

  test("Accumulating decoder returns as many errors as invalid elements in a NonEmptyList") {
    check { (nel: NonEmptyList[Either[String, Int]]) =>
      val json = nel.map(_.fold(Json.string, Json.int)).asJson
      val decoded = Decoder[NonEmptyList[Int]].accumulating(json.hcursor)
      val stringElems = nel.toList.collect { case elem if elem.isLeft => elem.left.get }

      decoded.fold(err => err.tail.size + 1, _ => 0) === stringElems.size
    }
  }

  test("Accumulating decoder returns as many errors as invalid elements in a case class") {
    case class Sample(a: Int, b: Boolean, c: Int)
    case class BadSample(a: String, b: String, c: String)

    check { (a: Int, b: Boolean, c: Int) =>
      val json = new Sample(a, b, c).asJson
      val decoded = Decoder[BadSample].accumulating(json.hcursor)

      decoded.fold(err => err.tail.size + 1, _ => 0) === 3
    }
  }

  //TODO: add tests replaying cursor history to ensure it is correct (next push)
  ignore("Accumulating decoder error messages name all the invalid elements in a Json Array") {
    fail
  }
}
