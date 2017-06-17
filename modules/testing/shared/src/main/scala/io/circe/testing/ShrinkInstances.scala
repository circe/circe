package io.circe.testing

import io.circe.{ Json, JsonBigDecimal, JsonNumber, JsonObject }
import org.scalacheck.Shrink

private[testing] trait ShrinkInstances {
  private[this] val minNumberShrink = BigDecimal.valueOf(1L)
  private[this] val zero = BigDecimal.valueOf(0L)
  private[this] val two = BigDecimal.valueOf(2L)

  /**
   * Copied from ScalaCheck.
   */
  private[this] def interleave[T](xs: Stream[T], ys: Stream[T]): Stream[T] =
    if (xs.isEmpty) ys
    else if (ys.isEmpty) xs
    else xs.head #:: ys.head #:: interleave(xs.tail, ys.tail)

  implicit val shrinkJsonNumber: Shrink[JsonNumber] = Shrink { jn =>
    def halfs(n: BigDecimal): Stream[BigDecimal] =
      if (n < minNumberShrink) Stream.empty else n #:: halfs(n / two)

    jn.toBigDecimal match {
      case Some(n) =>
        val ns = if (n == zero) Stream.empty else {
          val hs = halfs(n / two).map(n - _)
          zero #:: interleave(hs, hs.map(h => -h))
        }

        ns.map(value => JsonBigDecimal(value.underlying))
      case None => Stream(jn)
    }
  }

  implicit val shrinkJsonObject: Shrink[JsonObject] = Shrink(o =>
    Shrink.shrinkContainer[List, (String, Json)].shrink(o.toList).map(JsonObject.fromIterable)
  )

  implicit val shrinkJson: Shrink[Json] = Shrink(
    _.fold(
      Stream.empty,
      _ => Stream.empty,
      n => shrinkJsonNumber.shrink(n).map(Json.fromJsonNumber),
      s => Shrink.shrinkString.shrink(s).map(Json.fromString),
      a =>
        if (a.size == 1) {
          shrinkJson.shrink(a.head)
        } else {
          Shrink.shrinkContainer[Vector, Json].shrink(a).map(Json.fromValues)
        },
      o =>
        if (o.size == 1) {
          shrinkJson.shrink(o.values.head)
        } else {
          shrinkJsonObject.shrink(o).map(Json.fromJsonObject)
        }
    )
  )
}
