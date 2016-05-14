package io.circe.optics

import algebra.Eq
import io.circe.{ Json, JsonNumber, JsonObject }
import io.circe.optics.all._
import io.circe.syntax._
import io.circe.tests.CirceSuite
import monocle.function.Plated.plate
import monocle.law.discipline.{ PrismTests, TraversalTests }
import monocle.law.discipline.function.{ AtTests, EachTests, FilterIndexTests, IndexTests }
import scalaz.Equal
import scalaz.std.anyVal._
import scalaz.std.math.bigDecimal._
import scalaz.std.math.bigInt._
import scalaz.std.option._
import scalaz.std.string._

class OpticsSuite extends CirceSuite {
  implicit val equalJson: Equal[Json] = Equal.equal(Eq[Json].eqv)
  implicit val equalJsonNumber: Equal[JsonNumber] = Equal.equal(Eq[JsonNumber].eqv)
  implicit val equalJsonObject: Equal[JsonObject] = Equal.equal(Eq[JsonObject].eqv)

  checkLaws("Json to Boolean", PrismTests(jsonBoolean))
  checkLaws("Json to BigDecimal", PrismTests(jsonBigDecimal))
  checkLaws("Json to Double", PrismTests(jsonDouble))
  checkLaws("Json to BigInt", PrismTests(jsonBigInt))
  checkLaws("Json to Long", PrismTests(jsonLong))
  checkLaws("Json to Int", PrismTests(jsonInt))
  checkLaws("Json to Short", PrismTests(jsonShort))
  checkLaws("Json to Byte", PrismTests(jsonByte))
  checkLaws("Json to String", PrismTests(jsonString))
  checkLaws("Json to JsonNumber", PrismTests(jsonNumber))
  checkLaws("Json to JsonObject", PrismTests(jsonObject))
  checkLaws("Json to List[Json]", PrismTests(jsonArray))

  checkLaws("JsonNumber to BigDecimal", PrismTests(jsonNumberBigDecimal))
  checkLaws("JsonNumber to BigInt", PrismTests(jsonNumberBigInt))
  checkLaws("JsonNumber to Long", PrismTests(jsonNumberLong))
  checkLaws("JsonNumber to Int", PrismTests(jsonNumberInt))
  checkLaws("JsonNumber to Short", PrismTests(jsonNumberShort))
  checkLaws("JsonNumber to Byte", PrismTests(jsonNumberByte))

  checkLaws("plated Json", TraversalTests(plate[Json]))

  checkLaws("objectEach", EachTests[JsonObject, Json])
  checkLaws("objectAt", AtTests[JsonObject, String, Option[Json]])
  checkLaws("objectIndex", IndexTests[JsonObject, String, Json])
  checkLaws("objectFilterIndex", FilterIndexTests[JsonObject, String, Json](_.size < 4))

  val json = Map("foo" -> Map("bar" -> List(1, 2, 3, 4, 5))).asJson

  // This is mostly a test for syntax, and more tests should be added for JsonPath.
  "JsonPath" should "support traversal by field name and array index" in {
    assert(JsonPath.root.foo.bar.at(0).json.getOption(json) === Some(1.asJson))
  }
}
