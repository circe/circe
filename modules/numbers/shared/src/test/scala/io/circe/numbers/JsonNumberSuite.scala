package io.circe.numbers

import io.circe.numbers.testing.{ IntegralString, JsonNumberString }
import java.math.BigDecimal
import org.scalatest.FlatSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scala.math.{ BigDecimal => SBigDecimal }
import scala.util.Try

class JsonNumberSuite extends FlatSpec with GeneratorDrivenPropertyChecks {
  implicit override val generatorDrivenConfig = PropertyCheckConfiguration(
    minSuccessful = 1000,
    sizeRange = 10000
  )

  private[this] def doubleEqv(x: Double, y: Double): Boolean = java.lang.Double.compare(x, y) == 0
  private[this] def trailingZeros(i: BigInt): Int = i.toString.reverse.takeWhile(_ == '0').size
  private[this] def significantDigits(i: BigInt): Int = i.toString.size - trailingZeros(i)

  "fromDouble(0)" should "equal fromBigDecimal(ZERO) (#348)" in {
    assert(JsonNumber.fromDouble(0) === JsonNumber.fromBigDecimal(BigDecimal.ZERO))
  }

  "fromDouble" should "round-trip Double values" in forAll { (value: Double) =>
    val d = JsonNumber.fromDouble(value)

    assert(
      doubleEqv(d.toDouble, value) && d.toBigDecimal.exists { roundTripped =>
        doubleEqv(roundTripped.doubleValue, value)
      }
    )
  }

  it should "round-trip negative zero" in {
    val d = JsonNumber.fromDouble(-0.0)

    assert(doubleEqv(d.toDouble, -0.0))
  }

  "signum" should "agree with BigInteger" in forAll { (value: BigInt) =>
    val d = JsonNumber.fromBigInteger(value.bigInteger)

    assert(d.signum == value.signum)
  }

  it should "agree with BigDecimal" in forAll { (value: SBigDecimal) =>
    val d = JsonNumber.fromBigDecimal(value.bigDecimal)

    assert(d.signum == value.signum)
  }

  it should "agree with Long" in forAll { (value: Long) =>
    val d = JsonNumber.fromLong(value)

    assert(d.signum == value.signum)
  }

  it should "agree with Double" in forAll { (value: Double) =>
    val d = JsonNumber.fromDouble(value)

    assert(d.signum == value.signum)
  }

  "fromLong" should "round-trip Long values" in forAll { (value: Long) =>
    val d = JsonNumber.fromLong(value)

    assert(d.toBigDecimal.map(_.longValue) === Some(value))
  }

  "toLong" should "round-trip Long values" in forAll { (value: Long) =>
    val d = JsonNumber.fromLong(value)

    assert(d.toLong === Some(value))
  }

  "truncateToLong" should "work on values too big to be represented exactly as doubles" in {
    val Some(d) = JsonNumber.parseJsonNumber("16858240615609565")

    assert(d.truncateToLong === 16858240615609565L)
  }

  it should "work on negative values too big to be represented exactly as doubles" in {
    val Some(d) = JsonNumber.parseJsonNumber("-16858240615609565")

    assert(d.truncateToLong === -16858240615609565L)
  }

  it should "work on values too small to be parsed as BigDecimals" in {
    val Some(d) = JsonNumber.parseJsonNumber("1e-16858240615609565")

    assert(d.truncateToLong === 0L)
  }

  it should "work on negative values too small to be parsed as BigDecimals" in {
    val Some(d) = JsonNumber.parseJsonNumber("-1e-16858240615609565")

    assert(d.truncateToLong === 0L)
  }

  it should "work on values larger than Long.MaxValue" in {
    val Some(d) = JsonNumber.parseJsonNumber("9223372036854775808")

    assert(d.truncateToLong === Long.MaxValue)
  }

  it should "work on negative values smaller than Long.MinValue" in {
    val Some(d) = JsonNumber.parseJsonNumber("-9223372036854775809")

    assert(d.truncateToLong === Long.MinValue)
  }

  "toBigInteger" should "fail on very large values" in {
    val Some(d) = JsonNumber.parseJsonNumber("1e262144")

    assert(d.toBigInteger === None)
  }

  it should "not count the sign against the digit length" in {
    val Some(d) = JsonNumber.parseJsonNumber("-1e262143")

    assert(d.toBigInteger === Some(new BigDecimal("-1e262143").toBigInteger))
  }

  "fromLong and fromDouble" should "agree on Int-sized integral values" in forAll { (value: Int) =>
    val dl = JsonNumber.fromLong(value.toLong)
    val dd = JsonNumber.fromDouble(value.toDouble)

    assert(dl === dd)
  }

  "fromBigDecimal" should "round-trip BigDecimal values" in forAll { (value: SBigDecimal) =>
    val result = JsonNumber.fromBigDecimal(value.underlying)

    assert(
      Try(new BigDecimal(value.toString)).toOption.forall { parsedValue =>
        result.toBigDecimal.exists { roundTripped =>
          roundTripped.compareTo(parsedValue) == 0
        }
      }
    )
  }

  /**
   * This is a workaround for a Scala.js bug that causes `BigDecimal` values
   * with sufficiently large exponents to be printed with negative exponents.
   *
   * The filter below will have no effect on JVM tests since the condition is
   * clearly nonsense.
   */
  private[this] def isBadJsBigDecimal(d: SBigDecimal): Boolean =
    d.abs > 1 && d.toString.contains("E-")

  it should "agree with parseJsonNumberUnsafe" in forAll { (value: SBigDecimal) =>
    whenever (!isBadJsBigDecimal(value)) {
      val expected = JsonNumber.parseJsonNumberUnsafe(value.toString)

      assert(JsonNumber.fromBigDecimal(value.bigDecimal) === expected)
    }
  }

  it should "agree with parseJsonNumberUnsafe on multiples of ten with trailing zeros" in {
    val bigDecimal = new BigDecimal("10.0")
    val fromBigDecimal = JsonNumber.fromBigDecimal(bigDecimal)
    val fromString = JsonNumber.parseJsonNumberUnsafe(bigDecimal.toString)

    assert(fromBigDecimal === fromString)
  }

  it should "work correctly on values whose string representations have exponents larger than Int.MaxValue" in {
    val bigDecimal = new BigDecimal("-17014118346046923173168730371588410572800E+2147483647")
    val fromBigDecimal = JsonNumber.fromBigDecimal(bigDecimal)
    val fromString = JsonNumber.parseJsonNumberUnsafe(bigDecimal.toString)

    assert(fromBigDecimal === fromString)
  }

  "fromBigInteger" should "round-trip BigInteger values" in forAll { (value: BigInt) =>
    assert(JsonNumber.fromBigInteger(value.underlying).toBigInteger === Some(value.underlying))
  }
  
  "integralIsValidLong" should "agree with toLong" in forAll { (input: IntegralString) =>
    assert(JsonNumber.integralIsValidLong(input.value) === Try(input.value.toLong).isSuccess)
  }

  "parseJsonNumber" should "parse any BigDecimal string" in forAll { (value: SBigDecimal) =>
    val d = JsonNumber.parseJsonNumber(value.toString)

    assert(
      d.nonEmpty && Try(new BigDecimal(value.toString)).toOption.forall { parsedValue =>
        d.flatMap(_.toBigDecimal).exists { roundTripped =>
          roundTripped.compareTo(parsedValue) == 0
        }
      }
    )
  }

  it should "parse number strings with big exponents" in {
    forAll { (integral: BigInt, fractionalDigits: BigInt, exponent: BigInt) =>
      val fractional = fractionalDigits.abs
      val s = s"$integral.${ fractional }e$exponent"

      val scale = -exponent + (
        (integral == 0, fractional == 0) match {
          case (true, true) => 0
          case (_, true) => -trailingZeros(integral)
          case (_, _) => significantDigits(fractional)
        }
      )

      (JsonNumber.parseJsonNumber(s), Try(new BigDecimal(s)).toOption) match {
        case (Some(parsedJsonNumber), Some(parsedBigDecimal)) if scale.isValidInt =>
          assert(parsedJsonNumber.toBigDecimal.exists(_.compareTo(parsedBigDecimal) == 0))
        case (Some(_), None) => assert(true)
        case _ => assert(false)
      }
    }
  }

  it should "parse JSON numbers" in forAll { (jns: JsonNumberString) =>
    assert(JsonNumber.parseJsonNumber(jns.value).nonEmpty)
  }

  it should "fail on bad input" in {
    val badNumbers = List("", "x", "01", "1x", "1ex", "1.0x", "1.x", "1e-x", "1e-0x", "1.", "1e", "1e-")

    badNumbers.foreach { input =>
      assert(JsonNumber.parseJsonNumber(input) === None)
    }
  }

  it should "round-trip Byte" in forAll { (b: Byte) =>
    assert(JsonNumber.parseJsonNumber(b.toString).flatMap(_.toByte) === Some(b))
  }

  it should "round-trip Short" in forAll { (s: Short) =>
    assert(JsonNumber.parseJsonNumber(s.toString).flatMap(_.toShort) === Some(s))
  }

  it should "round-trip Int" in forAll { (i: Int) =>
    assert(JsonNumber.parseJsonNumber(i.toString).flatMap(_.toInt) === Some(i))
  }

  it should "round-trip Long" in forAll { (l: Long) =>
    assert(JsonNumber.parseJsonNumber(l.toString).flatMap(_.toLong) === Some(l))
  }

  "toByte" should "fail on out-of-range values" in forAll { (l: Long) =>
    val invalid = l > Byte.MaxValue || l < Byte.MinValue

    assert(JsonNumber.parseJsonNumber(l.toString).flatMap(_.toByte).isEmpty === invalid)
  }

  "toShort" should "fail on out-of-range values" in forAll { (l: Long) =>
    val invalid = l > Short.MaxValue || l < Short.MinValue

    assert(JsonNumber.parseJsonNumber(l.toString).flatMap(_.toShort).isEmpty === invalid)
  }

  "toInt" should "fail on out-of-range values" in forAll { (l: Long) =>
    val invalid = l > Int.MaxValue || l < Int.MinValue

    assert(JsonNumber.parseJsonNumber(l.toString).flatMap(_.toInt).isEmpty === invalid)
  }

  "lazyJsonNumberUnsafe" should "match parseJsonNumberUnsafe" in forAll { (jns: JsonNumberString) =>
    assert(JsonNumber.lazyJsonNumberUnsafe(jns.value) == JsonNumber.parseJsonNumberUnsafe(jns.value))
  }
}
