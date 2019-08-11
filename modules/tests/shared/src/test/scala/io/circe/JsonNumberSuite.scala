package io.circe

import io.circe.numbers.testing.JsonNumberString
import io.circe.tests.CirceSuite

class JsonNumberSuite extends CirceSuite {
  "fromString" should "parse valid JSON numbers" in forAll { (jsn: JsonNumberString) =>
    assert(JsonNumber.fromString(jsn.value).nonEmpty)
  }

  it should "match Json.fromDouble" in forAll { (d: Double) =>
    val expected = Json.fromDouble(d).flatMap(_.asNumber)

    assert(JsonNumber.fromString(d.toString) === expected)
  }

  it should "match Json.fromFloat" in forAll { (f: Float) =>
    val expected = Json.fromFloat(f).flatMap(_.asNumber)

    assert(JsonNumber.fromString(f.toString) === expected)
  }

  it should "match Json.fromFloat for Floats that don't have the same toString when Double-ed" in {
    val value = -4.9913575e19f

    assert(Json.fromFloat(value).flatMap(_.asNumber) === JsonNumber.fromString(value.toString))
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

  "toBigDecimal" should "produce correct results" in forAll { (n: JsonNumber) =>
    whenever(n != Json.fromDouble(-0.0).flatMap(_.asNumber).get) {
      assert(n.toBigDecimal.forall(value => Json.fromBigDecimal(value).asNumber.get == n))
    }
  }

  it should "work for zero with a large exponent" in {
    val n = JsonNumber.fromString(s"0e${Int.MaxValue.toLong + 2L}")

    assert(n.flatMap(_.toBigDecimal).get === BigDecimal(0))
  }

  it should "work for other bad numbers" in {
    import java.math.{ BigDecimal => JavaBigDecimal, BigInteger => JavaBigInteger }
    val badNumber = s"0.1e${Int.MaxValue.toLong + 2L}"
    val n = JsonNumber.fromString(badNumber)
    val expected = BigDecimal(new JavaBigDecimal(JavaBigInteger.ONE, Int.MinValue))

    assert(n.flatMap(_.toBigDecimal).get === expected)
  }

  "toBigInt" should "produce correct results" in forAll { (n: JsonNumber) =>
    whenever(n != Json.fromDouble(-0.0).flatMap(_.asNumber).get) {
      assert(n.toBigInt.forall(value => Json.fromBigInt(value).asNumber.get == n))
    }
  }

  it should "work for zero with a large exponent" in {
    val n = JsonNumber.fromString(s"0e${Int.MaxValue.toLong + 1L}")

    assert(n.flatMap(_.toBigInt).get === BigInt(0))
  }

  "JsonFloat.toLong" should "return None if outside of Long bounds" in forAll { (f: Float) =>
    if (f < Long.MinValue || f > Long.MaxValue) {
      assert(JsonFloat(f).toLong === None)
    }
  }

  "JsonFloat.toBigInt" should "return None if it loses precision" in forAll { (f: Float) =>
    val j = JsonFloat(f)
    val expected = j.toBiggerDecimal match {
      case d if d.isWhole => Some(BigDecimal(f.toString).toBigInt)
      case _              => None
    }
    assert(j.toBigInt === expected)
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

  it should "compare Float and Long" in forAll { (f: Float, l: Long) =>
    runCompareTest(JsonFloat, f, JsonLong, l)
  }

  it should "compare Float and Double" in forAll { (f: Float, d: Double) =>
    runCompareTest(JsonFloat, f, JsonDouble, d)
  }

  it should "compare Float and Float" in forAll { (f1: Float, f2: Float) =>
    runCompareTest(JsonFloat, f1, JsonFloat, f2)
  }

  private def runCompareTest[A, B](f1: A => JsonNumber, v1: A, f2: B => JsonNumber, v2: B) = {
    val n1 = f1(v1)
    val n2 = f2(v2)
    val expected = v1 == v2
    assert((n1 === n2) === expected)
    assert((n2 === n1) === expected)
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

  "toString" should "produce the same encoding as BigDecimal#toString" in forAll { (input: BigDecimal) =>
    assert(JsonNumber.fromString(input.toString).get.toString == input.toString)
    assert(Json.fromBigDecimal(input).asNumber.get.toString == input.toString)
  }

  it should "produce the same encoding as BigInt#toString" in forAll { (input: BigInt) =>
    assert(JsonNumber.fromString(input.toString).get.toString == input.toString)
    assert(Json.fromBigInt(input).asNumber.get.toString == input.toString)
  }
}
