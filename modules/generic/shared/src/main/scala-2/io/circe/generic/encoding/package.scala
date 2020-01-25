package io.circe.generic

package object encoding {
  @deprecated("Use DerivedAsObjectEncoder", "0.12.0")
  type DerivedObjectEncoder[A] = DerivedAsObjectEncoder[A]
  @deprecated("Use DerivedAsObjectEncoder", "0.12.0")
  val DerivedObjectEncoder = DerivedAsObjectEncoder

  @deprecated("Use ReprAsObjectEncoder", "0.12.0")
  type ReprObjectEncoder[A] = ReprAsObjectEncoder[A]
  @deprecated("Use ReprAsObjectEncoder", "0.12.0")
  val ReprObjectEncoder = ReprAsObjectEncoder
}
