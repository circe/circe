package io.circe.generic.extras

package object encoding {
  @deprecated("Use ConfiguredAsObjectEncoder", "0.12.0")
  type ConfiguredObjectEncoder[A] = ConfiguredAsObjectEncoder[A]
  @deprecated("Use ConfiguredAsObjectEncoder", "0.12.0")
  val ConfiguredObjectEncoder = ConfiguredAsObjectEncoder

  @deprecated("Use ReprAsObjectEncoder", "0.12.0")
  type ReprObjectEncoder[A] = ReprAsObjectEncoder[A]
  @deprecated("Use ReprAsObjectEncoder", "0.12.0")
  val ReprObjectEncoder = ReprAsObjectEncoder
}
