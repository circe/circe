package io.circe.util

import io.circe.tests.{ CirceSuite, JsonNumberString }
import java.math.BigDecimal
import scala.math.{ BigDecimal => SBigDecimal }
import scala.util.Try

class NumberParsingSuite extends CirceSuite {
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

  test("parseBiggerDecimal with big exponents") {
    check { (integral: BigInt, fractional: BigInt, exponent: BigInt) =>
      val s = s"$integral.${ fractional.abs }e$exponent"
      val d = NumberParsing.parseBiggerDecimal(s)

      val scale = if (fractional != 0) -exponent else {
        Stream.iterate((integral, integral != 0 && integral % 10 == 0)) {
          case (i, powerOfTen) => (i / 10, i != 0 && i % 10 == 0)
        }.takeWhile(_._2).size - exponent
      }

      if (exponent == scale.isValidInt && (-scale).isValidInt) {
        d.flatMap(_.toBigDecimal).exists { roundTripped =>
          roundTripped.compareTo(new BigDecimal(s)) == 0
        }
      } else d.nonEmpty
    }
  }

  test("parseBiggerDecimal with JSON number") {
    check { (jns: JsonNumberString) =>
      NumberParsing.parseBiggerDecimal(jns.s).nonEmpty
    }
  }
}
