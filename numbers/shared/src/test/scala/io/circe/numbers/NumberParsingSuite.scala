package io.circe.numbers

import io.circe.tests.JsonNumberString
import java.math.BigDecimal
import org.scalatest.FunSuite
import org.scalatest.prop.Checkers
import scala.math.{ BigDecimal => SBigDecimal }
import scala.util.Try

class NumberParsingSuite extends FunSuite with Checkers {
  implicit override val generatorDrivenConfig = PropertyCheckConfiguration(
    minSuccessful = 1000,
    sizeRange = 10000
  )

  test("parseBiggerDecimal with BigDecimal string") {
    check { (value: SBigDecimal) =>
      val d = NumberParsing.parseBiggerDecimal(value.toString)

      d.nonEmpty && Try(new BigDecimal(value.toString)).toOption.forall { parsedValue =>
        d.flatMap(_.toBigDecimal).exists { roundTripped =>
          roundTripped.compareTo(parsedValue) == 0
        }
      }
    }
  }

  private[this] def trailingZeros(i: BigInt): Int = i.toString.reverse.takeWhile(_ == '0').size
  private[this] def significantDigits(i: BigInt): Int = i.toString.size - trailingZeros(i)

  test("parseBiggerDecimal with big exponents") {
    check { (integral: BigInt, fractionalDigits: BigInt, exponent: BigInt) =>
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
          parsedBiggerDecimal.toBigDecimal.exists(_.compareTo(parsedBigDecimal) == 0)
        case (Some(_), None) => true
        case _ => false
      }
    }
  }

  test("parseBiggerDecimal with JSON number") {
    check { (jns: JsonNumberString) =>
      NumberParsing.parseBiggerDecimal(jns.value).nonEmpty
    }
  }
}
