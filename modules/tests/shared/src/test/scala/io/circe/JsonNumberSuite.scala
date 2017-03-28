package io.circe

import io.circe.testing.JsonNumberString
import io.circe.tests.CirceSuite
import scala.math.{ min, max }

class JsonNumberSuite extends CirceSuite {
  "fromString" should "parse valid JSON numbers" in forAll { (jsn: JsonNumberString) =>
    assert(JsonNumber.fromString(jsn.value).nonEmpty)
  }

  it should "match Json.fromDouble" in forAll { (d: Double) =>
    assert(Json.fromDouble(d).flatMap(_.asNumber) === JsonNumber.fromString(d.toString))
  }

  it should "match Json.fromFloat" in forAll { (f: Float) =>
    assert(Json.fromFloat(f).flatMap(_.asNumber) === JsonNumber.fromString(f.toString))
  }

  it should "round-trip Byte" in forAll { (b: Byte) =>
    assert(JsonNumber.fromString(b.toString).flatMap(_.toByte) === Some(b))
  }

  it should "round-trip Short" in forAll { (s: Short) =>
    assert(JsonNumber.fromString(s.toString).flatMap(_.toShort) === Some(s))
  }

  it should "round-trip Int" in forAll { (i: Int) =>
    assert(JsonNumber.fromString(i.toString).flatMap(_.toInt) === Some(i))
  }

  it should "round-trip Long" in forAll { (l: Long) =>
    assert(JsonNumber.fromString(l.toString).flatMap(_.toLong) === Some(l))
  }

  "toByte" should "fail on out-of-range values" in forAll { (l: Long) =>
    val invalid = l > Byte.MaxValue || l < Byte.MinValue

    assert(JsonNumber.fromString(l.toString).flatMap(_.toByte).isEmpty === invalid)
  }

  "toShort" should "fail on out-of-range values" in forAll { (l: Long) =>
    val invalid = l > Short.MaxValue || l < Short.MinValue

    assert(JsonNumber.fromString(l.toString).flatMap(_.toShort).isEmpty === invalid)
  }

  "toInt" should "fail on out-of-range values" in forAll { (l: Long) =>
    val invalid = l > Int.MaxValue || l < Int.MinValue

    assert(JsonNumber.fromString(l.toString).flatMap(_.toInt).isEmpty === invalid)
  }

  "truncateToLong" should "round toward zero" in {
    assert(JsonNumber.fromString("1.5").map(_.truncateToLong) === Some(1L))
    assert(JsonNumber.fromString("-1.5").map(_.truncateToLong) === Some(-1L))
    assert(Json.fromDouble(1.5).flatMap(_.asNumber).map(_.truncateToLong) === Some(1L))
    assert(Json.fromDouble(-1.5).flatMap(_.asNumber).map(_.truncateToLong) === Some(-1L))
    assert(Json.fromFloat(1.5f).flatMap(_.asNumber).map(_.truncateToLong) === Some(1L))
    assert(Json.fromFloat(-1.5f).flatMap(_.asNumber).map(_.truncateToLong) === Some(-1L))
    assert(Json.fromBigDecimal(BigDecimal(1.5)).asNumber.map(_.truncateToLong) === Some(1L))
    assert(Json.fromBigDecimal(BigDecimal(-1.5)).asNumber.map(_.truncateToLong) === Some(-1L))
  }

  "truncateToByte" should "return the truncated value" in forAll { (l: Long) =>
    val truncated: Byte = min(Byte.MaxValue, max(Byte.MinValue, l)).toByte

    assert(JsonNumber.fromString(l.toString).map(_.truncateToByte) === Some(truncated))
  }

  "truncateToShort" should "return the truncated value" in forAll { (l: Long) =>
    val truncated: Short = min(Short.MaxValue, max(Short.MinValue, l)).toShort

    assert(JsonNumber.fromString(l.toString).map(_.truncateToShort) === Some(truncated))
  }

  "truncateToInt" should "return the truncated value" in forAll { (l: Long) =>
    val truncated: Int = min(Int.MaxValue, max(Int.MinValue, l)).toInt

    assert(JsonNumber.fromString(l.toString).map(_.truncateToInt) === Some(truncated))
  }

  "Eq[JsonNumber]" should "distinguish negative and positive zeros" in {
    assert(JsonNumber.fromIntegralStringUnsafe("-0") =!= JsonNumber.fromIntegralStringUnsafe("0"))
  }

  it should "distinguishes negative and positive zeros with fractional parts" in {
    assert(JsonNumber.fromDecimalStringUnsafe("-0.0") =!= JsonNumber.fromDecimalStringUnsafe("0.0"))
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

  "fromFloat" should "fail on Float.NaN" in {
    assert(Json.fromFloat(Float.NaN) === None)
  }

  it should "fail on Float.PositiveInfinity" in {
    assert(Json.fromFloat(Float.PositiveInfinity) === None)
  }

  it should "fail on Float.NegativeInfinity" in {
    assert(Json.fromFloat(Float.NegativeInfinity) === None)
  }

  "fromFloatOrNull" should "return Null on Float.NaN" in {
    assert(Json.fromFloatOrNull(Float.NaN) === Json.Null)
  }

  it should "return Null on Float.PositiveInfinity" in {
    assert(Json.fromFloatOrNull(Float.PositiveInfinity) === Json.Null)
  }

  it should "return NUll on Float.NegativeInfinity" in {
    assert(Json.fromFloatOrNull(Float.NegativeInfinity) === Json.Null)
  }

  "fromFloatOrString" should "return String on Float.NaN" in {
    assert(Json.fromFloatOrString(Float.NaN) === Json.JString("NaN"))
  }

  it should "return String on Float.PositiveInfinity" in {
    assert(Json.fromFloatOrString(Float.PositiveInfinity) === Json.JString("Infinity"))
  }

  it should "return String on Float.NegativeInfinity" in {
    assert(Json.fromFloatOrString(Float.NegativeInfinity) === Json.JString("-Infinity"))
  }

  it should "return JNumber for valid Floats" in {
    assert(Json.fromFloatOrString(1.1f) === Json.JNumber(JsonNumber.fromDecimalStringUnsafe("1.1")))
    assert(Json.fromFloatOrString(-1.2f) === Json.JNumber(JsonNumber.fromDecimalStringUnsafe("-1.2")))
  }
}
