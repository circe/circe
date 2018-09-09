package io.circe

import cats.data.Chain
import scala.collection.mutable.Builder

private[circe] class ChainBuilder[A] extends Builder[A, Chain[A]] {
  var xs: Chain[A] = Chain.nil

  def clear(): Unit = xs = Chain.nil

  def result(): Chain[A] = xs

  def +=(elem: A): this.type = {
    xs = xs.append(elem)
    this
  }
}
