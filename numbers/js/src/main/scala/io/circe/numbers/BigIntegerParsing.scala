package io.circe.numbers

import java.math.BigInteger

private[numbers] class BigIntegerParsing {
  final def parseBigInteger(input: String): BigInteger =
    new BigInteger(if (input.charAt(0) == '+') input.substring(1) else input)
}
