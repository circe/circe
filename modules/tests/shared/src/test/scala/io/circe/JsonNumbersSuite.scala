package io.circe

import io.circe.numbers.JsonNumber
import io.circe.tests.CirceSuite
import scala.math.{ min, max }

class JsonNumbersSuite extends CirceSuite {
  "Json.fromJsonNumber" should "match Json.fromDouble" in forAll { (d: Double) =>
    val expected = Json.fromDouble(d)

    assert(JsonNumber.parseJsonNumber(d.toString).map(Json.fromJsonNumber) === expected)
  }

  it should "match Json.fromFloat" in forAll { (f: Float) =>
    val expected = Json.fromFloat(f)

    assert(JsonNumber.parseJsonNumber(f.toString).map(Json.fromJsonNumber) === expected)
  }

  it should "match Json.fromFloat for Floats that don't have the same toString when Double-ed" in {
    val value = -4.9913575E19F
    val expected = Json.fromFloat(value)

    assert(JsonNumber.parseJsonNumber(value.toString).map(Json.fromJsonNumber) === expected)
  }

  "JFloat#asNumber and then toLong" should "return None if outside of Long bounds" in forAll { (f: Float) =>
    val expected = f < Long.MinValue || f > Long.MaxValue || !f.isWhole

    assert(Json.fromFloatOrNull(f).asNumber.exists(_.toLong.isEmpty) === expected)
  }

  "truncateToLong" should "round toward zero" in {
    assert(JsonNumber.parseJsonNumber("1.5").map(_.truncateToLong) === Some(1L))
    assert(JsonNumber.parseJsonNumber("-1.5").map(_.truncateToLong) === Some(-1L))
    assert(Json.fromDouble(1.5).flatMap(_.asNumber).map(_.truncateToLong) === Some(1L))
    assert(Json.fromDouble(-1.5).flatMap(_.asNumber).map(_.truncateToLong) === Some(-1L))
    assert(Json.fromFloat(1.5f).flatMap(_.asNumber).map(_.truncateToLong) === Some(1L))
    assert(Json.fromFloat(-1.5f).flatMap(_.asNumber).map(_.truncateToLong) === Some(-1L))
    assert(Json.fromBigDecimal(BigDecimal(1.5)).asNumber.map(_.truncateToLong) === Some(1L))
    assert(Json.fromBigDecimal(BigDecimal(-1.5)).asNumber.map(_.truncateToLong) === Some(-1L))
  }

  "truncateToByte" should "return the truncated value" in forAll { (l: Long) =>
    val truncated: Byte = min(Byte.MaxValue, max(Byte.MinValue, l)).toByte

    assert(JsonNumber.parseJsonNumber(l.toString).map(_.truncateToByte) === Some(truncated))
  }

  "truncateToShort" should "return the truncated value" in forAll { (l: Long) =>
    val truncated: Short = min(Short.MaxValue, max(Short.MinValue, l)).toShort

    assert(JsonNumber.parseJsonNumber(l.toString).map(_.truncateToShort) === Some(truncated))
  }

  "truncateToInt" should "return the truncated value" in forAll { (l: Long) =>
    val truncated: Int = min(Int.MaxValue, max(Int.MinValue, l)).toInt

    assert(JsonNumber.parseJsonNumber(l.toString).map(_.truncateToInt) === Some(truncated))
  }

  val positiveZeros: List[JsonNumber] = List(
    JsonNumber.parseJsonNumberUnsafe("0.0"),
    Json.fromDouble(0.0).flatMap(_.asNumber).get,
    Json.fromFloat(0.0f).flatMap(_.asNumber).get,
    Json.fromLong(0).asNumber.get,
    Json.fromBigInt(BigInt(0)).asNumber.get,
    Json.fromBigDecimal(BigDecimal(0)).asNumber.get
  )

  val negativeZeros: List[JsonNumber] = List(
    JsonNumber.parseJsonNumberUnsafe("-0.0"),
    Json.fromDouble(-0.0).flatMap(_.asNumber).get,
    Json.fromFloat(-0.0f).flatMap(_.asNumber).get
  )

  "Eq[JsonNumber]" should "distinguish negative and positive zeros" in {
    positiveZeros.foreach { pz =>
      negativeZeros.foreach { nz =>
        assert(pz =!= nz)
      }
    }
  }

  it should "not distinguish any positive zeros" in {
    positiveZeros.foreach { pz1 =>
      positiveZeros.foreach { pz2 =>
        assert(pz1 === pz2)
      }
    }
  }

  it should "not distinguish any negative zeros" in {
    negativeZeros.foreach { nz1 =>
      negativeZeros.foreach { nz2 =>
        assert(nz1 === nz2)
      }
    }
  }

  it should "compare Float and Double" in forAll { (d: Double) =>
    assert((Json.fromFloat(d.toFloat) === Json.fromDouble(d)) === (d.toFloat.toDouble === d))
  }

  it should "compare Float and Float" in forAll { (f: Float) =>
    assert(Json.fromFloat(f) === Json.fromFloat(f))
  }

  "fromDouble" should "fail on Double.NaN" in {
    assert(Json.fromDouble(Double.NaN) === None)
  }

  it should "fail on Double.PositiveInfinity" in {
    assert(Json.fromDouble(Double.PositiveInfinity) === None)
  }

  it should "fail on Double.NegativeInfinity" in {
    assert(Json.fromDouble(Double.NegativeInfinity) === None)
  }

  "fromFloat" should "fail on Float.Nan" in {
    assert(Json.fromFloat(Float.NaN) === None)
  }

  it should "fail on Float.PositiveInfinity" in {
    assert(Json.fromFloat(Float.PositiveInfinity) === None)
  }

  it should "fail on Float.NegativeInfinity" in {
    assert(Json.fromFloat(Float.NegativeInfinity) === None)
  }
}
