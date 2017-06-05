package io.circe.testing

import io.circe.numbers.JsonNumber
import java.math.BigDecimal
import scala.scalajs.js.JSON
import scala.util.Try

/**
 * We only want to generate arbitrary [[JsonNumber]] values that Scala.js can
 * parse.
 */
private[testing] trait ArbitraryJsonNumberTransformer {
  def transformJsonNumber(n: JsonNumber): JsonNumber =
    Try(JSON.parse(n.toString): Any).toOption.filter {
      case x: Double => !x.isInfinite && n.toBigDecimal.exists(_ == BigDecimal.valueOf(x))
      case _ => true
    }.fold(JsonNumber.parseJsonNumberUnsafe("0"))(_ => n)
}
