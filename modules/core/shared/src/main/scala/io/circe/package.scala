package io

package object circe {
  @deprecated("Use Encoder.AsRoot", "0.12.0")
  type RootEncoder[A] = Encoder.AsRoot[A]
  @deprecated("Use Encoder.AsRoot", "0.12.0")
  val RootEncoder = Encoder.AsRoot

  @deprecated("Use Encoder.AsArray", "0.12.0")
  type ArrayEncoder[A] = Encoder.AsArray[A]
  @deprecated("Use Encoder.AsArray", "0.12.0")
  val ArrayEncoder = Encoder.AsArray

  @deprecated("Use Encoder.AsObject", "0.12.0")
  type ObjectEncoder[A] = Encoder.AsObject[A]
  @deprecated("Use Encoder.AsObject", "0.12.0")
  val ObjectEncoder = Encoder.AsObject
}
