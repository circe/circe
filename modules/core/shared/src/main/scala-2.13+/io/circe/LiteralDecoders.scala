package io.circe

private[circe] trait LiteralDecoders {
  private[this] abstract class LiteralDecoder[A, L <: A](decodeA: Decoder[A], L: ValueOf[L]) extends Decoder[L] {
    protected[this] def check(a: A): Boolean
    protected[this] def message: String

    final def apply(c: HCursor): Decoder.Result[L] = decodeA(c) match {
      case r @ Right(value) if check(value) => r.asInstanceOf[Decoder.Result[L]]
      case _                                => Left(DecodingFailure(message, c.history))
    }
  }

  /**
   * Decode a `String` whose value is known at compile time.
   *
   * @group Literal
   */
  implicit final def decodeLiteralString[L <: String](implicit L: ValueOf[L]): Decoder[L] =
    new LiteralDecoder[String, L](Decoder.decodeString, L) {
      protected[this] final def check(a: String): Boolean = a == L.value
      protected[this] final def message: String = s"""String("${L.value}")"""
    }

  /**
   * Decode a `Double` whose value is known at compile time.
   *
   * @group Literal
   */
  implicit final def decodeLiteralDouble[L <: Double](implicit L: ValueOf[L]): Decoder[L] =
    new LiteralDecoder[Double, L](Decoder.decodeDouble, L) {
      protected[this] final def check(a: Double): Boolean = java.lang.Double.compare(a, L.value) == 0
      protected[this] final def message: String = s"""Double(${L.value})"""
    }

  /**
   * Decode a `Float` whose value is known at compile time.
   *
   * @group Literal
   */
  implicit final def decodeLiteralFloat[L <: Float](implicit L: ValueOf[L]): Decoder[L] =
    new LiteralDecoder[Float, L](Decoder.decodeFloat, L) {
      protected[this] final def check(a: Float): Boolean = java.lang.Float.compare(a, L.value) == 0
      protected[this] final def message: String = s"""Float(${L.value})"""
    }

  /**
   * Decode a `Long` whose value is known at compile time.
   *
   * @group Literal
   */
  implicit final def decodeLiteralLong[L <: Long](implicit L: ValueOf[L]): Decoder[L] =
    new LiteralDecoder[Long, L](Decoder.decodeLong, L) {
      protected[this] final def check(a: Long): Boolean = a == L.value
      protected[this] final def message: String = s"""Long(${L.value})"""
    }

  /**
   * Decode a `Int` whose value is known at compile time.
   *
   * @group Literal
   */
  implicit final def decodeLiteralInt[L <: Int](implicit L: ValueOf[L]): Decoder[L] =
    new LiteralDecoder[Int, L](Decoder.decodeInt, L) {
      protected[this] final def check(a: Int): Boolean = a == L.value
      protected[this] final def message: String = s"""Int(${L.value})"""
    }

  /**
   * Decode a `Char` whose value is known at compile time.
   *
   * @group Literal
   */
  implicit final def decodeLiteralChar[L <: Char](implicit L: ValueOf[L]): Decoder[L] =
    new LiteralDecoder[Char, L](Decoder.decodeChar, L) {
      protected[this] final def check(a: Char): Boolean = a == L.value
      protected[this] final def message: String = s"""Char(${L.value})"""
    }

  /**
   * Decode a `Boolean` whose value is known at compile time.
   *
   * @group Literal
   */
  implicit final def decodeLiteralBoolean[L <: Boolean](implicit L: ValueOf[L]): Decoder[L] =
    new LiteralDecoder[Boolean, L](Decoder.decodeBoolean, L) {
      protected[this] final def check(a: Boolean): Boolean = a == L.value
      protected[this] final def message: String = s"""Boolean(${L.value})"""
    }
}
