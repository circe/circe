package io.circe

private[circe] trait LiteralEncoders {
  private[this] final class LiteralEncoder[L](private[this] final val encoded: Json) extends Encoder[L] {
    final def apply(a: L): Json = encoded
  }

  /**
   * Encode a `String` whose value is known at compile time.
   *
   * @group Literal
   */
  implicit final def encodeLiteralString[L <: String](implicit L: ValueOf[L]): Encoder[L] =
    new LiteralEncoder[L](Encoder.encodeString(L.value))

  /**
   * Encode a `Double` whose value is known at compile time.
   *
   * @group Literal
   */
  implicit final def encodeLiteralDouble[L <: Double](implicit L: ValueOf[L]): Encoder[L] =
    new LiteralEncoder[L](Encoder.encodeDouble(L.value))

  /**
   * Encode a `Float` whose value is known at compile time.
   *
   * @group Literal
   */
  implicit final def encodeLiteralFloat[L <: Float](implicit L: ValueOf[L]): Encoder[L] =
    new LiteralEncoder[L](Encoder.encodeFloat(L.value))

  /**
   * Encode a `Long` whose value is known at compile time.
   *
   * @group Literal
   */
  implicit final def encodeLiteralLong[L <: Long](implicit L: ValueOf[L]): Encoder[L] =
    new LiteralEncoder[L](Encoder.encodeLong(L.value))

  /**
   * Encode a `Int` whose value is known at compile time.
   *
   * @group Literal
   */
  implicit final def encodeLiteralInt[L <: Int](implicit L: ValueOf[L]): Encoder[L] =
    new LiteralEncoder[L](Encoder.encodeInt(L.value))

  /**
   * Encode a `Char` whose value is known at compile time.
   *
   * @group Literal
   */
  implicit final def encodeLiteralChar[L <: Char](implicit L: ValueOf[L]): Encoder[L] =
    new LiteralEncoder[L](Encoder.encodeChar(L.value))

  /**
   * Encode a `Boolean` whose value is known at compile time.
   *
   * @group Literal
   */
  implicit final def encodeLiteralBoolean[L <: Boolean](implicit L: ValueOf[L]): Encoder[L] =
    new LiteralEncoder[L](Encoder.encodeBoolean(L.value))
}
