package io.circe.numbers

import io.circe.numbers.testing.{ IntegralString, JsonNumberString, ZeroString }
import java.math.{ BigDecimal => JavaBigDecimal, BigInteger }
import org.scalatest.{ FunSpec, Matchers }
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import scala.util.Try

class JsonNumberParserSuite extends FunSpec with Matchers with GeneratorDrivenPropertyChecks {
  implicit override val generatorDrivenConfig: PropertyCheckConfiguration = PropertyCheckConfiguration(
    minSuccessful = 1000,
    sizeRange = 10000
  )

  private def doubleEqv(x: Double, y: Double): Boolean = java.lang.Double.compare(x, y) == 0
  private def trailingZeros(value: BigInt): Int = value.toString.reverse.takeWhile(_ == '0').size
  private def significantDigits(value: BigInt): Int = value.toString.size - trailingZeros(value)

  /**
   * ScalaCheck generates `BigDecimal` values that can't be round-tripped through `toString`.
   */
  private def parseBigDecimalSafe(input: String): Option[BigDecimal] =
    Try(new JavaBigDecimal(input.toString)).toOption.map(new BigDecimal(_))

  /**
   * This is a workaround for a Scala.js bug that causes `BigDecimal` values with sufficiently large
   * exponents to be printed with negative exponents.
   *
   * The filter below will have no effect on JVM tests since the condition is clearly nonsense.
   */
  private def isBadJsBigDecimal(value: BigDecimal): Boolean =
    value.abs > BigDecimal(1) && value.toString.contains("E-")

  sealed trait SimpleJsonNumber
  case object NegativeZero extends SimpleJsonNumber
  case object UnsignedZero extends SimpleJsonNumber
  case class JsonLong(value: Long) extends SimpleJsonNumber
  case class JsonBigDecimal(unscaled: BigInteger, scale: Int) extends SimpleJsonNumber
  case class JsonBiggerDecimal(unscaled: BigInteger, scale: BigInteger) extends SimpleJsonNumber

  object SimpleJsonNumberParser extends JsonNumberParser[Option[SimpleJsonNumber]] {
    def createNegativeZeroValue: Option[SimpleJsonNumber] = Some(NegativeZero)
    def createUnsignedZeroValue: Option[SimpleJsonNumber] = Some(UnsignedZero)
    def createLongValue(value: Long): Option[SimpleJsonNumber] = Some(JsonLong(value))
    def createBigDecimalValue(unscaled: BigInteger, scale: Int): Option[SimpleJsonNumber] =
      Some(JsonBigDecimal(unscaled, scale))

    def createBiggerDecimalValue(unscaled: BigInteger, scale: BigInteger): Option[SimpleJsonNumber] =
      Some(JsonBiggerDecimal(unscaled, scale))

    def failureValue: Option[SimpleJsonNumber] = None
  }

  describe("JsonNumberParser") {
    describe("parse") {
      describe("should parse") {
        it("any valid JSON number") {
          forAll { (input: JsonNumberString) =>
            SimpleJsonNumberParser.parse(input.value) shouldBe defined
          }
        }

        it("any valid integral JSON number") {
          forAll { (input: IntegralString) =>
            val result = SimpleJsonNumberParser.parse(input.value)

            result.get match {
              case NegativeZero => ()
              case UnsignedZero => ()
              case JsonLong(_) => ()
              case JsonBigDecimal(_, scale) => scale should be <= 0
              case JsonBiggerDecimal(_, scale) => scale.signum should be <= 0
            }
          }
        }

        it("valid JSON zeros as zeros") {
          forAll { (input: ZeroString) =>
            val result = SimpleJsonNumberParser.parse(input.value)
            val expected = if (input.isNegative) NegativeZero else UnsignedZero

            result should contain (expected)
          }
        }
      }

      describe("should fail") {
        it("on empty strings") {
          SimpleJsonNumberParser.parse("") shouldBe empty
        }

        it("on a single negative siqn") {
          SimpleJsonNumberParser.parse("-") shouldBe empty
        }

        it("on garbage") {
          forAll { (input: String) =>
            SimpleJsonNumberParser.parse(s"${ input }x") shouldBe empty
          }
        }
      }
    }

    describe("parseUnsafe") {
      it("should never throw exceptions") {
        forAll { (input: String) =>
          SimpleJsonNumberParser.parseUnsafe(input)
        }
      }

      it("should fail on 1e922337203685477580x") {
        SimpleJsonNumberParser.parseUnsafe("1e922337203685477580x") shouldBe empty
      }
    }

    describe("parseUnsafeWithIndices") {
      it("should never throw exceptions") {
        forAll { (input: String, decIndex: Int, expIndex: Int) =>
          SimpleJsonNumberParser.parseUnsafeWithIndices(input, decIndex, expIndex)
        }
      }

      it("should never throw exceptions on known problematic inputs") {
        SimpleJsonNumberParser.parseUnsafeWithIndices(" ", -1, 0)
      }

      it("should fail on invalid indices") {
        forAll { (input: JsonNumberString) =>
          SimpleJsonNumberParser.parseUnsafeWithIndices(input.value, -1, input.value.length) shouldBe empty
          SimpleJsonNumberParser.parseUnsafeWithIndices(input.value, input.value.length, -1) shouldBe empty
        }
      }
    }

    describe("createLongValue") {
      it("should always be called for non-zero Long values") {
        forAll { (value: Long) =>
          val result = SimpleJsonNumberParser.parse(value.toString)
          val expected = if (value == 0L) UnsignedZero else JsonLong(value)

          result should contain (expected)
        }
      }
    }

    describe("fromLong") {
      it("should agree with parse") {
        forAll { (value: Long) =>
          val result = SimpleJsonNumberParser.fromLong(value)

          result shouldBe SimpleJsonNumberParser.parse(value.toString)
        }
      }
    }

    describe("fromDouble") {
      describe("should agree with parse") {
        it("on arbitrary values") {
          forAll { (value: Double) =>
            val result = SimpleJsonNumberParser.fromDouble(value)

            result shouldBe SimpleJsonNumberParser.parse(value.toString)
          }
        }

        it("on zero") {
          val result = SimpleJsonNumberParser.fromDouble(0.0)

          result shouldBe SimpleJsonNumberParser.parse("0.0")
        }

        it("on negative zero") {
          val result = SimpleJsonNumberParser.fromDouble(-0.0)

          result shouldBe SimpleJsonNumberParser.parse("-0.0")
        }
      }

      describe("should reject") {
        it("NaN") {
          SimpleJsonNumberParser.fromDouble(Double.NaN) shouldBe empty
        }

        it("positive infinity") {
          SimpleJsonNumberParser.fromDouble(Double.PositiveInfinity) shouldBe empty
        }

        it("negative infinity") {
          SimpleJsonNumberParser.fromDouble(Double.NegativeInfinity) shouldBe empty
        }
      }
    }

    describe("fromFloat") {
      describe("should agree with parse") {
        it("on arbitrary values") {
          forAll { (value: Float) =>
            val result = SimpleJsonNumberParser.fromFloat(value)

            result shouldBe SimpleJsonNumberParser.parse(value.toString)
          }
        }

        it("on zero") {
          val result = SimpleJsonNumberParser.fromFloat(0.0f)

          result shouldBe SimpleJsonNumberParser.parse("0.0")
        }

        it("on negative zero") {
          val result = SimpleJsonNumberParser.fromFloat(-0.0f)

          result shouldBe SimpleJsonNumberParser.parse("-0.0")
        }
      }

      describe("should reject") {
        it("NaN") {
          SimpleJsonNumberParser.fromFloat(Float.NaN) shouldBe empty
        }

        it("positive infinity") {
          SimpleJsonNumberParser.fromFloat(Float.PositiveInfinity) shouldBe empty
        }

        it("negative infinity") {
          SimpleJsonNumberParser.fromFloat(Float.NegativeInfinity) shouldBe empty
        }
      }
    }

    describe("fromBigDecimal") {
      it("should agree with parse") {
        forAll { (value: BigDecimal) =>
          val result = SimpleJsonNumberParser.fromBigDecimal(value.underlying)

          result shouldBe SimpleJsonNumberParser.parse(value.toString)
        }
      }

      it("should agree with parse on 1.0") {
        val result = SimpleJsonNumberParser.fromBigDecimal(new JavaBigDecimal("1.0"))

        result shouldBe SimpleJsonNumberParser.parse("1.0")
      }
    }

    describe("fromBigInteger") {
      it("should agree with parse") {
        forAll { (value: BigInt) =>
          val result = SimpleJsonNumberParser.fromBigInteger(value.underlying)

          result shouldBe SimpleJsonNumberParser.parse(value.toString)
        }
      }
    }
  }
}
