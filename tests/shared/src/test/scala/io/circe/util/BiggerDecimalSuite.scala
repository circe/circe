package io.circe.util

import io.circe.tests.CirceSuite
import java.math.BigDecimal
import scala.math.{ BigDecimal => SBigDecimal }
import scala.util.Try

class BiggerDecimalSuite extends CirceSuite {
  implicit override val generatorDrivenConfig = PropertyCheckConfiguration(
    minSuccessful = 1000,
    sizeRange = 10000
  )

  private[this] def doubleEqv(x: Double, y: Double): Boolean =
    java.lang.Double.compare(x, y) == 0

  test("fromDouble") {
    check { (value: Double) =>
      val d = BiggerDecimal.fromDouble(value)

      doubleEqv(d.toDouble, value) && d.toBigDecimal.exists { roundTripped =>
        doubleEqv(roundTripped.doubleValue, value)
      }
    }
  }

  test("fromDouble with negative zero") {
    val d = BiggerDecimal.fromDouble(-0.0)

    d.toBigDecimal.exists { roundTripped =>
      doubleEqv(roundTripped.doubleValue, -0.0)
    }
  }

  test("fromLong") {
    check { (value: Long) =>
      val d = BiggerDecimal.fromLong(value)

      d.toBigDecimal.map(_.longValue) === Some(value)
    }
  }

  test("fromBigDecimal") {
    check { (value: SBigDecimal) =>
      val d = BiggerDecimal.fromBigDecimal(value.underlying)

      Try(new BigDecimal(value.toString)).toOption.forall { parsedValue =>
        d.toBigDecimal.exists { roundTripped =>
          roundTripped.compareTo(parsedValue) == 0
        }
      }
    }
  }
}
