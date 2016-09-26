package io.circe.numbers

import java.math.BigDecimal
import org.scalatest.FlatSpec
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scala.math.{ BigDecimal => SBigDecimal }
import scala.util.Try

class BiggerDecimalSuite extends FlatSpec with GeneratorDrivenPropertyChecks {
  implicit override val generatorDrivenConfig = PropertyCheckConfiguration(
    minSuccessful = 1000,
    sizeRange = 10000
  )

  private[this] def doubleEqv(x: Double, y: Double): Boolean = java.lang.Double.compare(x, y) == 0

  "fromDouble(0)" should "equal fromBigDecimal(ZERO) (#348)" in {
    assert(BiggerDecimal.fromDouble(0) === BiggerDecimal.fromBigDecimal(BigDecimal.ZERO))
  }

  "fromDouble" should "round-trip Double values" in forAll { (value: Double) =>
    val d = BiggerDecimal.fromDouble(value)

    assert(
      doubleEqv(d.toDouble, value) && d.toBigDecimal.exists { roundTripped =>
        doubleEqv(roundTripped.doubleValue, value)
      }
    )
  }

  it should "round-trip negative zero" in {
    val d = BiggerDecimal.fromDouble(-0.0)

    assert(doubleEqv(d.toDouble, -0.0))
  }

  "fromLong" should "round-trip Long values" in forAll { (value: Long) =>
    val d = BiggerDecimal.fromLong(value)

    assert(d.toBigDecimal.map(_.longValue) === Some(value))
  }

  "toLong" should "round-trip Long values" in forAll { (value: Long) =>
    val d = BiggerDecimal.fromLong(value)

    assert(d.toLong === Some(value))
  }

  "fromLong and fromDouble" should "agree on Int-sized integral values" in forAll { (value: Int) =>
    val dl = BiggerDecimal.fromLong(value.toLong)
    val dd = BiggerDecimal.fromDouble(value.toDouble)

    assert(dl === dd)
  }

  "fromBigDecimal" should "round-trip BigDecimal values" in forAll { (value: SBigDecimal) =>
    val result = BiggerDecimal.fromBigDecimal(value.underlying)

    assert(
      Try(new BigDecimal(value.toString)).toOption.forall { parsedValue =>
        result.toBigDecimal.exists { roundTripped =>
          roundTripped.compareTo(parsedValue) == 0
        }
      }
    )
  }

  "fromBigInteger" should "round-trip BigInteger values" in forAll { (value: BigInt) =>
    assert(BiggerDecimal.fromBigInteger(value.underlying).toBigInteger === Some(value.underlying))
  }
}
