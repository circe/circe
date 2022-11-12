package io.circe.testing

import io.circe.JsonNumber

private[testing] trait ArbitraryJsonNumberTransformer {
  def transformJsonNumber(n: JsonNumber): JsonNumber = n
}
