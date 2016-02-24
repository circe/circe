package io.circe

import io.circe.tests.{ CirceSuite, JsonNumberString }
import scala.math.{ min, max }

class JsonNumberSuite extends CirceSuite {
  test("fromString") {
    check { (jsn: JsonNumberString) =>
      JsonNumber.fromString(jsn.value).nonEmpty
    }
  }

  test("fromString should match Json.fromDouble") {
    check { (d: Double) =>
      Json.fromDouble(d).flatMap(_.asNumber) === JsonNumber.fromString(d.toString)
    }
  }

  test("round-trip Byte") {
    check { (b: Byte) =>
      JsonNumber.fromString(b.toString).flatMap(_.toByte) === Some(b)
    }
  }

  test("round-trip Short") {
    check { (s: Short) =>
      JsonNumber.fromString(s.toString).flatMap(_.toShort) === Some(s)
    }
  }

  test("round-trip Int") {
    check { (i: Int) =>
      JsonNumber.fromString(i.toString).flatMap(_.toInt) === Some(i)
    }
  }

  test("round-trip Long") {
    check { (l: Long) =>
      JsonNumber.fromString(l.toString).flatMap(_.toLong) === Some(l)
    }
  }

  test("toByte failure") {
    check { (l: Long) =>
      JsonNumber.fromString(l.toString).flatMap(_.toByte).isEmpty ===
        l > Byte.MaxValue || l < Byte.MinValue
    }
  }

  test("toShort failure") {
    check { (l: Long) =>
      JsonNumber.fromString(l.toString).flatMap(_.toShort).isEmpty ===
        l > Short.MaxValue || l < Short.MinValue
    }
  }

  test("toInt failure") {
    check { (l: Long) =>
      JsonNumber.fromString(l.toString).flatMap(_.toInt).isEmpty ===
        l > Int.MaxValue || l < Int.MinValue
    }
  }

  test("truncateToByte") {
    check { (l: Long) =>
      val truncated: Byte = min(Byte.MaxValue, max(Byte.MinValue, l)).toByte
      JsonNumber.fromString(l.toString).map(_.truncateToByte) === Some(truncated)
    }
  }

  test("truncateToShort") {
    check { (l: Long) =>
      val truncated: Short = min(Short.MaxValue, max(Short.MinValue, l)).toShort
      JsonNumber.fromString(l.toString).map(_.truncateToShort) === Some(truncated)
    }
  }

  test("truncateToInt") {
    check { (l: Long) =>
      val truncated: Int = min(Int.MaxValue, max(Int.MinValue, l)).toInt
      JsonNumber.fromString(l.toString).map(_.truncateToInt) === Some(truncated)
    }
  }

  test("Eq[JsonNumber] distinguishes negative zeros") {
    assert(JsonNumber.unsafeIntegral("-0") =!= JsonNumber.unsafeIntegral("0"))
  }

  test("Eq[JsonNumber] distinguishes negative zeros with fractional parts") {
    assert(JsonNumber.unsafeDecimal("-0.0") =!= JsonNumber.unsafeDecimal("0.0"))
  }

  test("number is empty on Double.NaN") {
    assert(Json.fromDouble(Double.NaN) === None)
  }

  test("number is empty on Double.PositiveInfinity") {
    assert(Json.fromDouble(Double.PositiveInfinity) === None)
  }

  test("number is empty on Double.NegativeInfinity") {
    assert(Json.fromDouble(Double.NegativeInfinity) === None)
  }
}
