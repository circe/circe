package io.circe

import io.circe.tests.{ CirceSuite, JsonNumberString }
import scala.math.{ min, max }

class JsonNumberSuite extends CirceSuite {
  "fromString" should "parse valid JSON numbers" in forAll { (jsn: JsonNumberString) =>
    assert(JsonNumber.fromString(jsn.value).nonEmpty)
  }

  it should "match Json.fromDouble" in forAll { (d: Double) =>
    assert(Json.fromDouble(d).flatMap(_.asNumber) === JsonNumber.fromString(d.toString))
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
    assert(JsonNumber.unsafeIntegral("-0") =!= JsonNumber.unsafeIntegral("0"))
  }

  it should "distinguishes negative and positive zeros with fractional parts" in {
    assert(JsonNumber.unsafeDecimal("-0.0") =!= JsonNumber.unsafeDecimal("0.0"))
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
}
