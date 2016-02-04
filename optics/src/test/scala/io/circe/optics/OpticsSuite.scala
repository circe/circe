package io.circe.optics

import algebra.Eq
import io.circe.{ Json, JsonNumber, JsonObject }
import io.circe.optics.all._
import io.circe.syntax._
import io.circe.tests.CirceSuite
import monocle.function.Plated.plate
import monocle.law.discipline.{ PrismTests, TraversalTests }
import monocle.law.discipline.function.{ AtTests, EachTests, FilterIndexTests, IndexTests }
import org.scalatest.{ FunSuite, Matchers }
import org.typelevel.discipline.scalatest.Discipline
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

  checkAll("Json to Boolean", PrismTests(jsonBoolean))
  checkAll("Json to BigDecimal", PrismTests(jsonBigDecimal))
  checkAll("Json to Double", PrismTests(jsonDouble))
  checkAll("Json to BigInt", PrismTests(jsonBigInt))
  checkAll("Json to Long", PrismTests(jsonLong))
  checkAll("Json to Int", PrismTests(jsonInt))
  checkAll("Json to Short", PrismTests(jsonShort))
  checkAll("Json to Byte", PrismTests(jsonByte))
  checkAll("Json to String", PrismTests(jsonString))
  checkAll("Json to JsonNumber", PrismTests(jsonNumber))
  checkAll("Json to JsonObject", PrismTests(jsonObject))
  checkAll("Json to List[Json]", PrismTests(jsonArray))

  checkAll("JsonNumber to BigDecimal", PrismTests(jsonNumberBigDecimal))
  checkAll("JsonNumber to Double", PrismTests(jsonNumberDouble))
  checkAll("JsonNumber to BigInt", PrismTests(jsonNumberBigInt))
  checkAll("JsonNumber to Long", PrismTests(jsonNumberLong))
  checkAll("JsonNumber to Int", PrismTests(jsonNumberInt))
  checkAll("JsonNumber to Short", PrismTests(jsonNumberShort))
  checkAll("JsonNumber to Byte", PrismTests(jsonNumberByte))

  checkAll("plated Json", TraversalTests(plate[Json]))

  checkAll("objectEach", EachTests[JsonObject, Json])
  checkAll("objectAt", AtTests[JsonObject, String, Option[Json]]("foo"))
  checkAll("objectIndex", IndexTests[JsonObject, String, Json]("foo"))
  checkAll("objectFilterIndex", FilterIndexTests[JsonObject, String, Json](_.size < 4))

  val json = Map("foo" -> Map("bar" -> List(1, 2, 3, 4, 5))).asJson

  // This is mostly a test for syntax, and more tests should be added for JsonPath.
  test("JsonPath should support traversal by field name and array index") {
    JsonPath.root.foo.bar.at(0).json.getOption(json) === Some(1.asJson)
  }
}
