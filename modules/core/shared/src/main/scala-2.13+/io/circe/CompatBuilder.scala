package io.circe

import scala.collection.mutable.Builder

private[circe] abstract class CompatBuilder[A, C] extends Builder[A, C]
