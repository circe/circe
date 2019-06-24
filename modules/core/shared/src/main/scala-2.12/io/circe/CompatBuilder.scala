package io.circe

import scala.collection.mutable.Builder

private[circe] abstract class CompatBuilder[A, C] extends Builder[A, C] {
  def addOne(elem: A): this.type

  final def +=(elem: A): this.type = addOne(elem)
}
