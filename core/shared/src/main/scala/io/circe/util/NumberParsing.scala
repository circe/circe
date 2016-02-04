package io.circe.util

import java.math.{ BigDecimal, BigInteger }
import scala.annotation.switch

final object NumberParsing {
  private[this] final val MaxLongString = "9223372036854775807"
  private[this] final val MinLongString = "-9223372036854775808"

  /**
   * Is a string representing an integral value a valid [[scala.Long]]?
   */
  def integralIsValidLong(s: String): Boolean = {
    val bound = if (s.charAt(0) == '-') MinLongString else MaxLongString

    s.length < bound.length || (s.length == bound.length && s.compareTo(bound) <= 0)
  }

  private[this] final val FAILED = 0
  private[this] final val START = 1
  private[this] final val AFTER_ZERO = 2
  private[this] final val AFTER_DOT = 3
  private[this] final val FRACTIONAL = 4
  private[this] final val AFTER_E = 5
  private[this] final val AFTER_EXP_SIGN = 6
  private[this] final val EXPONENT = 7
  private[this] final val INTEGRAL = 8

  def parseBiggerDecimal(input: String): Option[BiggerDecimal] = {
    val len = input.length
    var zeros = 0
    var decIndex = -1
    var expIndex = -1
    var i = if (input.charAt(0) == '-') 1 else 0
    var c = input.charAt(i)

    var state = if (input.charAt(i) != '0') START else {
      i = i + 1
      AFTER_ZERO
    }

    while (i < len && state != FAILED) {
      val c = input.charAt(i)

      (state: @switch) match {
        case START =>
          if (c >= '1' && c <= '9') {
            state = INTEGRAL
          } else {
            state = FAILED
          }
        case AFTER_ZERO =>
          if (c == '.') {
            state = AFTER_DOT
          } else if (c == 'e' || c == 'E') {
            state = AFTER_E
          } else {
            state = FAILED
          }
        case INTEGRAL =>
          if (c == '0') {
            zeros = zeros + 1
            state = INTEGRAL
          } else if (c >= '1' && c <= '9') {
            zeros = 0
            state = INTEGRAL
          } else if (c == '.') {
            state = AFTER_DOT
          } else if (c == 'e' || c == 'E') {
            state = AFTER_E
          } else {
            state = FAILED
          }
        case AFTER_DOT =>
          decIndex = i - 1
          if (c == '0') {
            zeros = 1
            state = FRACTIONAL
          } else if (c >= '1' && c <= '9') {
            zeros = 0
            state = FRACTIONAL
          } else {
            state = FAILED
          }
        case AFTER_E =>
          expIndex = i - 1
          if (c >= '0' && c <= '9') {
            state = EXPONENT
          } else if (c == '+' || c == '-') {
            state = AFTER_EXP_SIGN
          } else {
            state = FAILED
          }
        case FRACTIONAL =>
          if (c == '0') {
            zeros = zeros + 1
            state = FRACTIONAL
          } else if (c >= '1' && c <= '9') {
            zeros = 0
            state = FRACTIONAL
          } else if (c == 'e' || c == 'E') {
            state = AFTER_E
          } else {
            state = FAILED
          }
        case AFTER_EXP_SIGN =>
          if (c >= '0' && c <= '9') {
            state = EXPONENT
          } else {
            state = FAILED
          }
        case EXPONENT =>
          if (c >= '0' && c <= '9') {
            state = EXPONENT
          } else {
            state = FAILED
          }
      }

      i += 1
    }

    if (state == FAILED) None else {
      val integral = if (decIndex >= 0) input.substring(0, decIndex) else {
        if (expIndex == -1) input else {
          input.substring(0, expIndex)
        }
      }

      val fractional = if (decIndex == -1) "" else {
        if (expIndex == -1) input.substring(decIndex + 1) else {
          input.substring(decIndex + 1, expIndex)
        }
      }

      val unscaledString = integral + fractional
      val unscaled = new BigInteger(unscaledString.substring(0, unscaledString.length - zeros))
      val rescale = BigInteger.valueOf(fractional.length.toLong - zeros)
      val exponent = if (expIndex == -1) BigInteger.ZERO else {
        new BigInteger(input.substring(expIndex + 1))
      }

      Some(
        if (input.charAt(0) == '-' && unscaled == BigInteger.ZERO) {
          BiggerDecimal.NegativeZero
        } else {
          new SigAndExp(
            unscaled,
            if (unscaled == BigInteger.ZERO) BigInteger.ZERO else rescale.subtract(exponent)
          )
        }
      )
    }
  }
}
