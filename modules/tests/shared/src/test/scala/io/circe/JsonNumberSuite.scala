package io.circe

import cats.kernel.instances.all._
import cats.syntax.eq._
import io.circe.numbers.testing.JsonNumberString
import io.circe.tests.CirceMunitSuite
import org.scalacheck.Prop
import org.scalacheck.Prop._

class JsonNumberSuite extends CirceMunitSuite {
  property("fromString should parse valid JSON numbers") {
    forAll { (jns: JsonNumberString) =>
      assert(JsonNumber.fromString(jns.value).nonEmpty)
    }
  }

  property("fromString  should match Json.fromDouble") {
    forAll { (d: Double) =>
      val expected = Json.fromDouble(d).flatMap(_.asNumber)
      JsonNumber.fromString(d.toString) ?= expected
    }
  }

  property("fromString should match Json.fromFloat") {
    forAll { (f: Float) =>
      val expected = Json.fromFloat(f).flatMap(_.asNumber)

      JsonNumber.fromString(f.toString) ?= expected
    }
  }

  test("fromString should match Json.fromFloat for Floats that don't have the same toString when Double-ed") {
    val value = -4.9913575e19f

    assertEquals(Json.fromFloat(value).flatMap(_.asNumber), JsonNumber.fromString(value.toString))
  }

  property("fromString should round-trip Byte") {
    forAll { (b: Byte) =>
      JsonNumber.fromString(b.toString).flatMap(_.toByte) ?= Some(b)
    }
  }

  property("fromString should round-trip Short") {
    forAll { (s: Short) =>
      JsonNumber.fromString(s.toString).flatMap(_.toShort) ?= Some(s)
    }
  }

  property("fromString should round-trip Int") {
    forAll { (i: Int) =>
      JsonNumber.fromString(i.toString).flatMap(_.toInt) ?= Some(i)
    }
  }

  property("fromString should round-trip Long") {
    forAll { (l: Long) =>
      JsonNumber.fromString(l.toString).flatMap(_.toLong) ?= Some(l)
    }
  }

  property("toByte should fail on out-of-range values") {
    forAll { (l: Long) =>
      val invalid = l > Byte.MaxValue || l < Byte.MinValue

      JsonNumber.fromString(l.toString).flatMap(_.toByte).isEmpty ?= invalid
    }
  }

  property("toShort should fail on out-of-range values") {
    forAll { (l: Long) =>
      val invalid = l > Short.MaxValue || l < Short.MinValue

      JsonNumber.fromString(l.toString).flatMap(_.toShort).isEmpty ?= invalid
    }
  }

  property("toInt should fail on out-of-range values") {
    forAll { (l: Long) =>
      val invalid = l > Int.MaxValue || l < Int.MinValue
      JsonNumber.fromString(l.toString).flatMap(_.toInt).isEmpty ?= invalid
    }
  }

  property("toBigDecimal should produce correct results") {
    forAll { (n: JsonNumber) =>
      if (n != Json.fromDouble(-0.0).flatMap(_.asNumber).get) {
        assert(n.toBigDecimal.forall(value => Json.fromBigDecimal(value).asNumber.get == n))
      }
    }
  }

  test("toBigDecimal should work for zero with a large exponent") {
    val n = JsonNumber.fromString(s"0e${Int.MaxValue.toLong + 2L}")
    assertEquals(n.flatMap(_.toBigDecimal).get, BigDecimal(0))
  }

  test("toBigDecimal should work for other bad numbers") {
    import java.math.{ BigDecimal => JavaBigDecimal, BigInteger => JavaBigInteger }
    val badNumber = s"0.1e${Int.MaxValue.toLong + 2L}"
    val n = JsonNumber.fromString(badNumber)
    val expected = BigDecimal(new JavaBigDecimal(JavaBigInteger.ONE, Int.MinValue))

    assertEquals(n.flatMap(_.toBigDecimal).get, expected)
  }

  property("toBigInt should produce correct results") {
    forAll { (n: JsonNumber) =>
      if (n != Json.fromDouble(-0.0).flatMap(_.asNumber).get) {
        assert(n.toBigInt.forall(value => Json.fromBigInt(value).asNumber.get == n))
      }
    }
  }

  test("toBigInt should work for zero with a large exponent") {
    val n = JsonNumber.fromString(s"0e${Int.MaxValue.toLong + 1L}")

    assertEquals(n.flatMap(_.toBigInt).get, BigInt(0))
  }

  property("JsonFloat.toLong should return None if outside of Long bounds") {
    forAll { (f: Float) =>
      if (f < Long.MinValue || f > Long.MaxValue) {
        JsonFloat(f).toLong ?= None
      } else {
        Prop.undecided
      }
    }
  }

  property("JsonFloat.toBigInt should return None if it loses precision")(toBigIntProp)
  private lazy val toBigIntProp = forAll { (f: Float) =>
    val j = JsonFloat(f)
    val expected = j.toBiggerDecimal match {
      case d if d.isWhole => Some(BigDecimal(f.toString).toBigInt)
      case _              => None
    }
    j.toBigInt ?= expected
  }

  val positiveZeros: List[JsonNumber] = List(
    JsonNumber.fromIntegralStringUnsafe("0"),
    JsonNumber.fromDecimalStringUnsafe("0.0"),
    Json.fromDouble(0.0).flatMap(_.asNumber).get,
    Json.fromFloat(0.0f).flatMap(_.asNumber).get,
    Json.fromLong(0).asNumber.get,
    Json.fromBigInt(BigInt(0)).asNumber.get,
    Json.fromBigDecimal(BigDecimal(0)).asNumber.get
  )

  val negativeZeros: List[JsonNumber] = List(
    JsonNumber.fromIntegralStringUnsafe("-0"),
    JsonNumber.fromDecimalStringUnsafe("-0.0"),
    Json.fromDouble(-0.0).flatMap(_.asNumber).get,
    Json.fromFloat(-0.0f).flatMap(_.asNumber).get
  )

  test("Eq[JsonNumber] should distinguish negative and positive zeros") {
    positiveZeros.foreach { pz =>
      negativeZeros.foreach { nz =>
        assert(pz =!= nz)
      }
    }
  }

  test("Eq[JsonNumber] should not distinguish any positive zeros") {
    positiveZeros.foreach { pz1 =>
      positiveZeros.foreach { pz2 =>
        assertEquals(pz1, pz2)
      }
    }
  }

  test("Eq[JsonNumber] should not distinguish any negative zeros") {
    negativeZeros.foreach { nz1 =>
      negativeZeros.foreach { nz2 =>
        assertEquals(nz1, nz2)
      }
    }
  }

  property("Eq[JsonNumber] should compare Float and Long") {
    forAll { (f: Float, l: Long) =>
      runCompareTest(JsonFloat, f, JsonLong, l)
    }
  }

  property("Eq[JsonNumber] should compare Float and Double") {
    forAll { (f: Float, d: Double) =>
      runCompareTest(JsonFloat, f, JsonDouble, d)
    }
  }

  property("Eq[JsonNumber] should compare Float and Float") {
    forAll { (f1: Float, f2: Float) =>
      runCompareTest(JsonFloat, f1, JsonFloat, f2)
    }
  }

  private def runCompareTest[A, B](f1: A => JsonNumber, v1: A, f2: B => JsonNumber, v2: B) = {
    val n1 = f1(v1)
    val n2 = f2(v2)
    val expected = v1 == v2
    assert((n1 === n2) === expected)
    assert((n2 === n1) === expected)
  }

  test("fromDouble should fail on Double.NaN") {
    assertEquals(Json.fromDouble(Double.NaN), None)
  }

  test("fromDouble should fail on Double.PositiveInfinity") {
    assertEquals(Json.fromDouble(Double.PositiveInfinity), None)
  }

  test("fromDouble should fail on Double.NegativeInfinity") {
    assertEquals(Json.fromDouble(Double.NegativeInfinity), None)
  }

  test("fromFloat should fail on Float.Nan") {
    assertEquals(Json.fromFloat(Float.NaN), None)
  }

  test("fromFloat should fail on Float.PositiveInfinity") {
    assertEquals(Json.fromFloat(Float.PositiveInfinity), None)
  }

  test("fromFloat should fail on Float.NegativeInfinity") {
    assertEquals(Json.fromFloat(Float.NegativeInfinity), None)
  }

  property("toString should produce the same encoding as BigDecimal#toString") {
    forAll { (input: BigDecimal) =>
      JsonNumber.fromString(input.toString).get.toString ?= input.toString
      Json.fromBigDecimal(input).asNumber.get.toString ?= input.toString
    }
  }

  property("toString should produce the same encoding as BigInt#toString") {
    forAll { (input: BigInt) =>
      JsonNumber.fromString(input.toString).get.toString ?= input.toString
      Json.fromBigInt(input).asNumber.get.toString ?= input.toString
    }
  }
}
