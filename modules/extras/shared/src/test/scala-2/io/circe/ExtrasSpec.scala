package io.circe

import io.circe.syntax._
import io.circe.tests.CirceMunitSuite
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Prop.forAll

class ExtrasSpec extends CirceMunitSuite {

  property("sanitizeKeys should return input JSON if all of the JSON Object's keys are in the approvedList") {
    forAll[Set[String], String, Boolean](Gen.listOf(Gen.alphaNumStr).map(_.toSet), Gen.alphaNumStr) {
      (keys: Set[String], str: String) =>

        val withValues: Set[(String, Json)] =
          keys.map { (s: String) => (s, Json.JString(str)) }

        val input: Json = Json.fromJsonObject {
          JsonObject.fromMap(withValues.toMap)
        }

        val output: Json =
          extras.sanitizeKeys(
            input,
            keys,
            _ => ???,
            Json.fromString("not used"),
            _ => ???,
            _ => ???
          )

        input == output
    }
  }

  property("sanitizeKeys should return sanitized values for keys' values of a JSON Object") {
    forAll[Boolean, String, JsonNumber, Boolean](
      Arbitrary.arbBool.arbitrary,
      Gen.alphaNumStr,
      Arbitrary.arbitrary[JsonNumber]
    ) { (bool: Boolean, str: String, number: JsonNumber) =>

      def onBoolean(b: Boolean): Json = Json.fromBoolean(!b)
      val onNull: Json = Json.fromDoubleOrNull(42)
      def onString(s: String): Json = Json.fromString(s.reverse)
      def onNumber(s: JsonNumber): Json = Json.Null

      val input: Json = Json.obj(
        "hi" := bool,
        "there" := Json.Null,
        "hola" := str,
        "good" := Json.obj(
          "ciao" := number,
          "hi" := number
        )
      )

      val expected: Json = Json.obj(
        "hi" := !bool,
        "there" := Json.fromDoubleOrNull(42),
        "hola" := str,
        "good" := Json.obj(
          "ciao" := Json.Null,
          "hi" := Json.Null
        )
      )

      val output: Json =
        extras.sanitizeKeys(input, Set("hola"), onBoolean, onNull, onString, onNumber)

      expected == output
    }
  }

  property("sanitizeKeys should sanitize each value within an array") {
    forAll[Boolean, String, JsonNumber, Boolean](
      Arbitrary.arbBool.arbitrary,
      Gen.alphaNumStr,
      Arbitrary.arbitrary[JsonNumber]
    ) { (bool: Boolean, str: String, number: JsonNumber) =>

      def onBoolean(b: Boolean): Json = Json.fromBoolean(!b)
      val onNull: Json = Json.fromDoubleOrNull(42)
      def onString(s: String): Json = Json.fromString(s.reverse)
      def onNumber(s: JsonNumber): Json = Json.Null

      val input: Json = Json.fromValues {
        List(
          Json.obj(
            "hi" := bool,
            "there" := Json.Null,
            "hola" := str,
            "good" := Json.obj(
              "ciao" := number,
              "hi" := number
            )
          ),
          Json.fromBoolean(bool),
          Json.Null,
          Json.fromString(str),
          Json.fromJsonNumber(number)
        )
      }

      val expected: Json = Json.fromValues {
        List(
          Json.obj(
            "hi" := !bool,
            "there" := Json.fromDoubleOrNull(42),
            "hola" := str,
            "good" := Json.obj(
              "ciao" := Json.Null,
              "hi" := Json.Null
            )
          ),
          Json.fromBoolean(!bool),
          Json.fromDoubleOrNull(42),
          Json.fromString(str.reverse),
          Json.Null
        )
      }

      val output: Json =
        extras.sanitizeKeys(input, Set("hola"), onBoolean, onNull, onString, onNumber)

      expected == output
    }
  }

  property("sanitizeKeys should sanitize arrays and objects of approved and non-approved keys' values") {
    forAll[Boolean, String, Boolean](
      Arbitrary.arbBool.arbitrary,
      Gen.alphaNumStr
    ) { (bool: Boolean, str: String) =>

      def onBoolean(b: Boolean): Json = Json.fromBoolean(!b)
      def onString(s: String): Json = Json.fromString(s.reverse)

      val inputInnerObj: Json = Json.obj(
        "approved" := bool,
        "non-approved" := str
      )

      val input: Json = Json.obj(
        "approvedObject" := inputInnerObj,
        "nonapprovedObject" := inputInnerObj,
        "approvedArray" := Json.arr(inputInnerObj),
        "nonapprovedArray" := Json.arr(inputInnerObj)
      )

      val expectedInnerObj: Json = Json.obj(
        "approved" := bool,
        "non-approved" := str.reverse
      )

      val expected: Json = Json.obj(
        "approvedObject" := expectedInnerObj,
        "nonapprovedObject" := expectedInnerObj,
        "approvedArray" := Json.arr(expectedInnerObj),
        "nonapprovedArray" := Json.arr(expectedInnerObj)
      )

      val output: Json =
        extras.sanitizeKeys(
          input,
          Set("approvedObject", "approvedArray", "approved"),
          onBoolean,
          Json.Null,
          onString,
          _ => ???
        )

      expected == output
    }
  }
}
