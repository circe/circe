package io.circe.testing

import io.circe.numbers.JsonNumber

private[testing] trait ArbitraryJsonNumberTransformer {
  def transformJsonNumber(n: JsonNumber): JsonNumber = n
}
