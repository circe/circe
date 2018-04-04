package io.circe.optics

import cats.kernel.{Eq, Hash, Order}
import io.circe.optics.all._
import io.circe.tests.CirceSuite
import io.circe.{Json, JsonNumber, JsonObject}
import monocle.function.Plated.plate
import monocle.syntax.all._
import cats.instances.bigDecimal._
import cats.instances.bigInt._
import cats.instances.option._
import cats.instances.string._
import cats.instances.vector._
import cats.kernel.instances.DoubleOrder

class OpticsSuite extends CirceSuite {

  /**
   * For the purposes of these tests we consider `Double.NaN` to be equal to
   * itself.
   */
  override implicit val catsKernelStdOrderForDouble: Order[Double] with Hash[Double] =
    new DoubleOrder{
      override def eqv(a: Double, b: Double): Boolean =
        (a.isNaN && b.isNaN) || super.eqv(a, b)
    }

  checkLaws("Json to Unit", LawsTests.prismTests(jsonNull))
  checkLaws("Json to Boolean", LawsTests.prismTests(jsonBoolean))
  checkLaws("Json to BigDecimal", LawsTests.prismTests(jsonBigDecimal))
  checkLaws("Json to Double", LawsTests.prismTests(jsonDouble))
  checkLaws("Json to BigInt", LawsTests.prismTests(jsonBigInt))
  checkLaws("Json to Long", LawsTests.prismTests(jsonLong))
  checkLaws("Json to Int", LawsTests.prismTests(jsonInt))
  checkLaws("Json to Short", LawsTests.prismTests(jsonShort))
  checkLaws("Json to Byte", LawsTests.prismTests(jsonByte))
  checkLaws("Json to String", LawsTests.prismTests(jsonString))
  checkLaws("Json to JsonNumber", LawsTests.prismTests(jsonNumber))
  checkLaws("Json to JsonObject", LawsTests.prismTests(jsonObject))
  checkLaws("Json to Vector[Json]", LawsTests.prismTests(jsonArray))

  checkLaws("JsonNumber to BigDecimal", LawsTests.prismTests(jsonNumberBigDecimal))
  checkLaws("JsonNumber to BigInt", LawsTests.prismTests(jsonNumberBigInt))
  checkLaws("JsonNumber to Long", LawsTests.prismTests(jsonNumberLong))
  checkLaws("JsonNumber to Int", LawsTests.prismTests(jsonNumberInt))
  checkLaws("JsonNumber to Short", LawsTests.prismTests(jsonNumberShort))
  checkLaws("JsonNumber to Byte", LawsTests.prismTests(jsonNumberByte))

  checkLaws("plated Json", LawsTests.traversalTests(plate[Json]))

  checkLaws("jsonObjectEach", LawsTests.eachTests[JsonObject, Json])
  checkLaws("jsonObjectAt", LawsTests.atTests[JsonObject, String, Option[Json]])
  checkLaws("jsonObjectIndex", LawsTests.indexTests[JsonObject, String, Json])
  checkLaws("jsonObjectFilterIndex", LawsTests.filterIndexTests[JsonObject, String, Json])

  "jsonDouble" should "round-trip in reverse with Double.NaN" in {
    assert(jsonDouble.getOption(jsonDouble.reverseGet(Double.NaN)) === Some(Double.NaN))
  }

  it should "partial round-trip with numbers larger than Double.MaxValue" in {
    val json = Json.fromJsonNumber(JsonNumber.fromString((BigDecimal(Double.MaxValue) + 1).toString).get)

    assert(jsonDouble.getOrModify(json).fold(identity, jsonDouble.reverseGet) === json)
  }

  "jsonObjectFields" should "fold over all fields" in forAll { (obj: JsonObject) =>
    assert(obj.applyFold(JsonObjectOptics.jsonObjectFields).foldMap(List(_)) === obj.toList)
  }
}
