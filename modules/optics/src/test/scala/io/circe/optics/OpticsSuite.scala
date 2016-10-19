package io.circe.optics

import cats.Eq
import io.circe.optics.all._
import io.circe.tests.CirceSuite
import io.circe.{ Json, JsonNumber, JsonObject }
import monocle.function.Plated.plate
import monocle.law.discipline.function.{ AtTests, EachTests, FilterIndexTests, IndexTests }
import monocle.law.discipline.{ PrismTests, TraversalTests }
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

  /**
   * For the purposes of these tests we consider `Double.NaN` to be equal to
   * itself.
   */
  implicit val doubleInstance: Equal[Double] = Equal.equal { (a, b) =>
    (a.isNaN && b.isNaN) || scalaz.std.anyVal.doubleInstance.equal(a, b)
  }

  checkLaws("Json to Unit", PrismTests(jsonNull))
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
  checkLaws("objectFilterIndex", FilterIndexTests[JsonObject, String, Json])

  "jsonDouble" should "round-trip in reverse with Double.NaN" in {
    assert(jsonDouble.getOption(jsonDouble.reverseGet(Double.NaN)) === Some(Double.NaN))
  }

  it should "partial round-trip with numbers larger than Double.MaxValue" in {
    val json = Json.fromJsonNumber(JsonNumber.fromString((BigDecimal(Double.MaxValue) + 1).toString).get)

    assert(jsonDouble.getOrModify(json).fold(identity, jsonDouble.reverseGet) === json)
  }
}
