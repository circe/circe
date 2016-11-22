package io.circe.testing

import io.circe.ast.JsonNumber

private[testing] trait ArbitraryJsonNumberTransformer {
  def transformJsonNumber(n: JsonNumber): JsonNumber = n
}
