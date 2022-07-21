package io.circe.numbers

import io.circe.numbers.testing.{ IntegralString, JsonNumberString }
import java.math.{ BigDecimal, BigInteger }
import scala.math.{ BigDecimal => SBigDecimal }
import scala.util.Try
import munit.ScalaCheckSuite
import org.scalacheck._
import org.scalacheck.Prop._

class BiggerDecimalSuite extends ScalaCheckSuite {
  override def scalaCheckTestParameters =
    super.scalaCheckTestParameters.withMinSuccessfulTests(1000)

  private[this] def doubleEqv(x: Double, y: Double): Boolean = java.lang.Double.compare(x, y) == 0
  private[this] def trailingZeros(i: BigInt): Int = i.toString.reverse.takeWhile(_ == '0').size
  private[this] def significantDigits(i: BigInt): Int = i.toString.size - trailingZeros(i)

  private[this] val ZERO: SBigDecimal = SBigDecimal(0)

  private[this] def toStringWithLeadingZeros(leadingZeros: Int, value: SBigDecimal): String =
    if (value < ZERO) {
      s"-${List.fill(leadingZeros)('0').mkString}${value.abs}"
    } else {
      s"${List.fill(leadingZeros)('0').mkString}${value}"
    }

  test("fromDoubleUnsafe(0) should equal fromBigDecimal(ZERO) (#348)") {
    assertEquals(BiggerDecimal.fromDoubleUnsafe(0), BiggerDecimal.fromBigDecimal(BigDecimal.ZERO))
  }

  property("fromDoubleUnsafe should round-trip Double values") {
    forAll { (value: Double) =>
      val d = BiggerDecimal.fromDoubleUnsafe(value)

      assert(doubleEqv(d.toDouble, value))
      assert(d.toBigDecimal.exists(roundTripped => doubleEqv(roundTripped.doubleValue, value)))
    }
  }

  test("it should round-trip negative zero") {
    val d = BiggerDecimal.fromDoubleUnsafe(-0.0)
    assert(doubleEqv(d.toDouble, -0.0))
  }

  test("signum should agree with BigInteger") {
    forAll { (value: BigInt) =>
      val d = BiggerDecimal.fromBigInteger(value.underlying)

      assert(d.signum == value.signum)
    }
  }

  test("it should agree with BigDecimal") {
    forAll { (value: SBigDecimal) =>
      val d = BiggerDecimal.fromBigDecimal(value.underlying)
      assertEquals(d.signum, value.signum)
    }
  }

  test("it should agree with Long") {
    forAll { (value: Long) =>
      val d = BiggerDecimal.fromLong(value)

      val expected = value.signum
      assertEquals(d.signum, expected)
    }
  }

  test("it should agree with Double") {
    forAll { (value: Double) =>
      val d = BiggerDecimal.fromDoubleUnsafe(value)

      val expected = value.signum
      assert(d.signum == expected)
    }
  }

  property("fromLong should round-trip Long values") {
    forAll { (value: Long) =>
      val d = BiggerDecimal.fromLong(value)

      assertEquals(d.toBigDecimal.map(_.longValue), Some(value))
    }
  }

  property("toLong should round-trip Long values") {
    forAll { (value: Long) =>
      val d = BiggerDecimal.fromLong(value)

      assertEquals(d.toLong, Some(value))
    }
  }

  test("toBigInteger should fail on very large values") {
    val Some(d) = BiggerDecimal.parseBiggerDecimal("1e262144")

    assertEquals(d.toBigInteger, None)
  }

  test("it should not count the sign against the digit length") {
    val Some(d) = BiggerDecimal.parseBiggerDecimal("-1e262143")

    assertEquals(d.toBigInteger, Some(new BigDecimal("-1e262143").toBigInteger))
  }

  test("toBigIntegerWithMaxDigits should fail on values whose representation is too large") {
    val Some(d) = BiggerDecimal.parseBiggerDecimal("123456789")

    assertEquals(d.toBigIntegerWithMaxDigits(BigInteger.valueOf(8L)), None)
  }

  test("it should succeed when the representation is exactly the maximum size") {
    val Some(d) = BiggerDecimal.parseBiggerDecimal("123456789")

    assertEquals(d.toBigIntegerWithMaxDigits(BigInteger.valueOf(9L)), d.toBigInteger)
  }

  property("fromLong and fromDoubleUnsafe should agree on Int-sized integral values") {
    forAll { (value: Int) =>
      val dl = BiggerDecimal.fromLong(value.toLong)
      val dd = BiggerDecimal.fromDoubleUnsafe(value.toDouble)
      assertEquals(dl, dd)
    }
  }

  property("fromBigDecimal should round-trip BigDecimal values") {
    forAll { (value: SBigDecimal) =>
      val result = BiggerDecimal.fromBigDecimal(value.underlying)
      assert(
        Try(new BigDecimal(value.toString)).toOption.forall { parsedValue =>
          result.toBigDecimal.exists { roundTripped =>
            roundTripped.compareTo(parsedValue) == 0
          }
        }
      )
    }
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

  property("it should agree with parseBiggerDecimalUnsafe") {
    forAll { (value: SBigDecimal) =>
      if (!isBadJsBigDecimal(value)) {
        val expected = BiggerDecimal.parseBiggerDecimalUnsafe(value.toString)
        assertEquals(BiggerDecimal.fromBigDecimal(value.underlying), expected)
      }
    }
  }

  test("agree with parseBiggerDecimalUnsafe on 0.000") {
    val value = "0.000"
    val expected = BiggerDecimal.parseBiggerDecimalUnsafe(value)

    assertEquals(BiggerDecimal.fromBigDecimal(new BigDecimal(value)), expected)
  }

  test("it should agree with parseBiggerDecimalUnsafe on multiples of ten with trailing zeros") {
    val bigDecimal = new BigDecimal("10.0")
    val fromBigDecimal = BiggerDecimal.fromBigDecimal(bigDecimal)
    val fromString = BiggerDecimal.parseBiggerDecimalUnsafe(bigDecimal.toString)

    assertEquals(fromBigDecimal, fromString)
  }

  test("it should work correctly on values whose string representations have exponents larger than Int.MaxValue") {
    val bigDecimal = new BigDecimal("-17014118346046923173168730371588410572800E+2147483647")
    val fromBigDecimal = BiggerDecimal.fromBigDecimal(bigDecimal)
    val fromString = BiggerDecimal.parseBiggerDecimalUnsafe(bigDecimal.toString)

    assertEquals(fromBigDecimal, fromString)
  }

  property("fromBigInteger should round-trip BigInteger values") {
    forAll { (value: BigInt) =>
      assertEquals(BiggerDecimal.fromBigInteger(value.underlying).toBigInteger, Some(value.underlying))
    }
  }

  property("integralIsValidLong should agree with toLong") {
    forAll { (input: IntegralString) =>
      assertEquals(BiggerDecimal.integralIsValidLong(input.value), Try(input.value.toLong).isSuccess)
    }
  }

  property("parseBiggerDecimal should parse any BigDecimal string") {
    forAll { (value: SBigDecimal) =>
      val d = BiggerDecimal.parseBiggerDecimal(value.toString)

      assert(
        d.nonEmpty && Try(new BigDecimal(value.toString)).toOption.forall { parsedValue =>
          d.flatMap(_.toBigDecimal).exists { roundTripped =>
            roundTripped.compareTo(parsedValue) == 0
          }
        }
      )
    }
  }

  property("parse number strings with big exponents") {
    forAll { (integral: BigInt, fractionalDigits: BigInt, exponent: BigInt) =>
      val fractional = fractionalDigits.abs
      val s = s"$integral.${fractional}e$exponent"

      val scale = -exponent + (
        (integral == 0, fractional == 0) match {
          case (true, true) => 0
          case (_, true)    => -trailingZeros(integral)
          case (_, _)       => significantDigits(fractional)
        }
      )

      (BiggerDecimal.parseBiggerDecimal(s), Try(new BigDecimal(s)).toOption) match {
        case (Some(parsedBiggerDecimal), Some(parsedBigDecimal)) if scale.isValidInt =>
          assert(parsedBiggerDecimal.toBigDecimal.exists(_.compareTo(parsedBigDecimal) == 0))
        case (Some(_), None) => assert(true)
        case _               => assert(false)
      }
    }
  }

  property("it should parse JSON numbers") {
    forAll { (jns: JsonNumberString) =>
      assert(BiggerDecimal.parseBiggerDecimal(jns.value).nonEmpty)
    }
  }

  property("it should parse integral JSON numbers") {
    forAll { (is: IntegralString) =>
      val bb = BiggerDecimal.fromBigInteger(new BigInteger(is.value))
      assertEquals(BiggerDecimal.parseBiggerDecimal(is.value), Some(bb))
    }
  }

  test("it should fail on bad input") {
    val badNumbers = List("", "x", "1x", "1ex", "1.0x", "1.x", "1e-x", "1e-0x", "1.", "1e", "1e-", "-")

    badNumbers.foreach { input =>
      assert(BiggerDecimal.parseBiggerDecimal(input).isEmpty)
    }
  }

  property("leading zeros should be parseable") {
    forAll(Gen.choose(1, 100), Arbitrary.arbitrary[SBigDecimal]) { (leadingZeros: Int, bd: SBigDecimal) =>
      BiggerDecimal.parseBiggerDecimal(toStringWithLeadingZeros(leadingZeros, bd)) ?= Some(
        BiggerDecimal.fromBigDecimal(bd.bigDecimal)
      )
    }
  }
}
