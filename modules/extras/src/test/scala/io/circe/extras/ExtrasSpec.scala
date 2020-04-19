package io.circe.extras

import io.circe.{Json, JsonNumber, JsonObject}
import io.circe.syntax._
import io.circe.tests.CirceSuite
import org.scalacheck.{Arbitrary, Gen}

class ExtrasSpec extends CirceSuite {

  "sanitizeKeys" should "return input JSON if all of the JSON Object's keys are in the whitelist" in {
    forAll[Set[String], String, Boolean](Gen.listOf(Gen.alphaNumStr).map(_.toSet), Gen.alphaNumStr) {
      (keys: Set[String], str: String) =>

        val withValues: Set[(String, Json)] =
          keys.map { s: String => (s, Json.JString(str))}

        val input: Json = Json.fromJsonObject {
          JsonObject.fromMap(withValues.toMap)
        }

        val output: Json =
          Extras.sanitizeKeys(
            input,
            keys,
            _ => ???,
            Json.fromString(("not used")),
            _ => ???,
            _ => ???
          )

        input == output
      }
  }

  "sanitizeKeys" should "return sanitized values for keys' values of a JSON Object" in {
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
        Extras.sanitizeKeys(input, Set("hola"), onBoolean, onNull, onString, onNumber)

      expected == output
    }
  }

  "sanitizeKeys" should "santize each value within an array" in {
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
          Json.fromBoolean(bool),
          Json.Null,
          Json.fromString(str),
          Json.fromJsonNumber(number)
        )
      }

      val output: Json =
        Extras.sanitizeKeys(input, Set("hola"), onBoolean, onNull, onString, onNumber)

      expected == output
    }
  }
}
