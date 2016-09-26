package io.circe.numbers

import io.circe.testing.{ IntegralString, JsonNumberString }
import java.math.BigDecimal
import org.scalatest.FlatSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scala.math.{ BigDecimal => SBigDecimal }
import scala.util.Try

class NumberParsingSuite extends FlatSpec with GeneratorDrivenPropertyChecks {
  implicit override val generatorDrivenConfig = PropertyCheckConfiguration(
    minSuccessful = 1000,
    sizeRange = 10000
  )

  private[this] def trailingZeros(i: BigInt): Int = i.toString.reverse.takeWhile(_ == '0').size
  private[this] def significantDigits(i: BigInt): Int = i.toString.size - trailingZeros(i)

  "integralIsValidLong" should "agree with toLong" in forAll { (input: IntegralString) =>
    assert(NumberParsing.integralIsValidLong(input.value) === Try(input.value.toLong).isSuccess)
  }

  "parseBiggerDecimal" should "parse any BigDecimal string" in forAll { (value: SBigDecimal) =>
    val d = NumberParsing.parseBiggerDecimal(value.toString)

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

      (NumberParsing.parseBiggerDecimal(s), Try(new BigDecimal(s)).toOption) match {
        case (Some(parsedBiggerDecimal), Some(parsedBigDecimal)) if scale.isValidInt =>
          assert(parsedBiggerDecimal.toBigDecimal.exists(_.compareTo(parsedBigDecimal) == 0))
        case (Some(_), None) => assert(true)
        case _ => assert(false)
      }
    }
  }

  it should "parse JSON numbers" in forAll { (jns: JsonNumberString) =>
    assert(NumberParsing.parseBiggerDecimal(jns.value).nonEmpty)
  }

  it should "fail on bad input" in {
    val badNumbers = List("", "x", "01", "1x", "1ex", "1.0x", "1.x", "1e-x", "1e-0x")

    badNumbers.foreach { input =>
      assert(NumberParsing.parseBiggerDecimal(input) === None)
    }
  }
}
