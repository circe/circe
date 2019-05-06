package io.circe.numbers

import io.circe.numbers.testing.{ IntegralString, JsonNumberString }
import java.math.{ BigDecimal, BigInteger }
import org.scalacheck.{ Prop, Properties, Test }
import org.typelevel.claimant.Claim
import scala.math.{ BigDecimal => SBigDecimal }
import scala.util.Try

class BiggerDecimalSuite extends Properties("BiggerDecimal") {
  override def overrideParameters(p: Test.Parameters): Test.Parameters =
    p.withMinSuccessfulTests(1000).withMaxSize(10000)

  private[this] def doubleEqv(x: Double, y: Double): Boolean = java.lang.Double.compare(x, y) == 0
  private[this] def trailingZeros(i: BigInt): Int = i.toString.reverse.takeWhile(_ == '0').size
  private[this] def significantDigits(i: BigInt): Int = i.toString.size - trailingZeros(i)

  property("fromDoubleUnsafe(0) equal fromBigDecimal(ZERO) (#348)") = Claim(
    BiggerDecimal.fromDoubleUnsafe(0) == BiggerDecimal.fromBigDecimal(BigDecimal.ZERO)
  )

  property("fromDoubleUnsafe should round-trip Double values") = Prop.forAll { (value: Double) =>
    val d = BiggerDecimal.fromDoubleUnsafe(value)

    Claim(
      doubleEqv(d.toDouble, value) && d.toBigDecimal.exists { roundTripped =>
        doubleEqv(roundTripped.doubleValue, value)
      }
    )
  }

  property("fromDoubleUnsafe should round-trip negative zero") = {
    val d = BiggerDecimal.fromDoubleUnsafe(-0.0)

    Claim(doubleEqv(d.toDouble, -0.0))
  }

  property("signum should agree with BigInteger") = Prop.forAll { (value: BigInt) =>
    val d = BiggerDecimal.fromBigInteger(value.underlying)

    Claim(d.signum == value.signum)
  }

  property("signum should agree with BigDecimal") = Prop.forAll { (value: SBigDecimal) =>
    val d = BiggerDecimal.fromBigDecimal(value.underlying)

    Claim(d.signum == value.signum)
  }

  property("signum should agree with Long") = Prop.forAll { (value: Long) =>
    val d = BiggerDecimal.fromLong(value)

    Claim(d.signum == value.signum)
  }

  property("signum should agree with Double") = Prop.forAll { (value: Double) =>
    val d = BiggerDecimal.fromDoubleUnsafe(value)

    Claim(d.signum == value.signum)
  }

  property("fromLong should round-trip Long values") = Prop.forAll { (value: Long) =>
    val d = BiggerDecimal.fromLong(value)

    Claim(d.toBigDecimal.map(_.longValue) == Some(value))
  }

  property("toLong should round-trip Long values") = Prop.forAll { (value: Long) =>
    val d = BiggerDecimal.fromLong(value)

    Claim(d.toLong == Some(value))
  }

  property("toBigInteger should fail on very large values") = {
    val Some(d) = BiggerDecimal.parseBiggerDecimal("1e262144")

    Claim(d.toBigInteger == None)
  }

  property("toBigInteger should not count the sign against the digit length") = {
    val Some(d) = BiggerDecimal.parseBiggerDecimal("-1e262143")

    Claim(d.toBigInteger == Some(new BigDecimal("-1e262143").toBigInteger))
  }

  property("toBigIntegerWithMaxDigits should fail on values whose representation is too large") = {
    val Some(d) = BiggerDecimal.parseBiggerDecimal("123456789")

    Claim(d.toBigIntegerWithMaxDigits(BigInteger.valueOf(8L)) == None)
  }

  property("toBigIntegerWithMaxDigits should succeed when the representation is exactly the maximum size") = {
    val Some(d) = BiggerDecimal.parseBiggerDecimal("123456789")

    Claim(d.toBigIntegerWithMaxDigits(BigInteger.valueOf(9L)) == d.toBigInteger)
  }

  property("fromLong and fromDoubleUnsafe should agree on Int-sized integral values") = Prop.forAll { (value: Int) =>
    val dl = BiggerDecimal.fromLong(value.toLong)
    val dd = BiggerDecimal.fromDoubleUnsafe(value.toDouble)

    Claim(dl == dd)
  }

  property("fromBigDecimal should round-trip BigDecimal values") = Prop.forAll { (value: SBigDecimal) =>
    val result = BiggerDecimal.fromBigDecimal(value.underlying)

    Claim(
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

  property("fromBigDecimal should agree with parseBiggerDecimalUnsafe") = Prop.forAll { (value: SBigDecimal) =>
    val expected = BiggerDecimal.parseBiggerDecimalUnsafe(value.toString)

    Claim(isBadJsBigDecimal(value) || BiggerDecimal.fromBigDecimal(value.underlying) == expected)
  }

  property("fromBigDecimal should agree with parseBiggerDecimalUnsafe on 0.000") = {
    val value = "0.000"
    val expected = BiggerDecimal.parseBiggerDecimalUnsafe(value)

    Claim(BiggerDecimal.fromBigDecimal(new BigDecimal(value)) == expected)
  }

  property("fromBigDecimal should agree with parseBiggerDecimalUnsafe on multiples of ten with trailing zeros") = {
    val bigDecimal = new BigDecimal("10.0")
    val fromBigDecimal = BiggerDecimal.fromBigDecimal(bigDecimal)
    val fromString = BiggerDecimal.parseBiggerDecimalUnsafe(bigDecimal.toString)

    Claim(fromBigDecimal == fromString)
  }

  property(
    "fromBigDecimal should work on values whose string representations have exponents larger than Int.MaxValue"
  ) = {
    val bigDecimal = new BigDecimal("-17014118346046923173168730371588410572800E+2147483647")
    val fromBigDecimal = BiggerDecimal.fromBigDecimal(bigDecimal)
    val fromString = BiggerDecimal.parseBiggerDecimalUnsafe(bigDecimal.toString)

    Claim(fromBigDecimal == fromString)
  }

  property("fromBigInteger should round-trip BigInteger values") = Prop.forAll { (value: BigInt) =>
    Claim(BiggerDecimal.fromBigInteger(value.underlying).toBigInteger == Some(value.underlying))
  }

  property("integralIsValidLong should agree with toLong") = Prop.forAll { (input: IntegralString) =>
    Claim(BiggerDecimal.integralIsValidLong(input.value) == Try(input.value.toLong).isSuccess)
  }

  property("parseBiggerDecimal should parse any BigDecimal string") = Prop.forAll { (value: SBigDecimal) =>
    val d = BiggerDecimal.parseBiggerDecimal(value.toString)

    Claim(
      d.nonEmpty && Try(new BigDecimal(value.toString)).toOption.forall { parsedValue =>
        d.flatMap(_.toBigDecimal).exists { roundTripped =>
          roundTripped.compareTo(parsedValue) == 0
        }
      }
    )
  }

  property("parseBiggerDecimal should parse number strings with big exponents") = Prop.forAll {
    (integral: BigInt, fractionalDigits: BigInt, exponent: BigInt) =>
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
          Claim(parsedBiggerDecimal.toBigDecimal.exists(_.compareTo(parsedBigDecimal) == 0))
        case (Some(_), None) => Claim(true)
        case _               => Claim(false)
      }
  }

  property("parseBiggerDecimal should parse JSON numbers") = Prop.forAll { (jns: JsonNumberString) =>
    Claim(BiggerDecimal.parseBiggerDecimal(jns.value).nonEmpty)
  }

  property("parseBiggerDecimal should parse integral JSON numbers") = Prop.forAll { (is: IntegralString) =>
    Claim(BiggerDecimal.parseBiggerDecimal(is.value) == Some(BiggerDecimal.fromBigInteger(new BigInteger(is.value))))
  }

  property("parseBiggerDecimal should fail on bad input") = {
    val badNumbers = List("", "x", "01", "1x", "1ex", "1.0x", "1.x", "1e-x", "1e-0x", "1.", "1e", "1e-", "-")

    Claim(
      badNumbers.forall { input =>
        BiggerDecimal.parseBiggerDecimal(input) == None
      }
    )
  }
}
