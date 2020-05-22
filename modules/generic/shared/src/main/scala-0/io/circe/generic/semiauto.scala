package io.circe.generic

import io.circe.{ Codec, Decoder, Encoder }
import scala.deriving.Mirror

/**
 * Semi-automatic codec derivation.
 *
 * This object provides helpers for creating [[io.circe.Decoder]] and [[io.circe.ObjectEncoder]]
 * instances for case classes, "incomplete" case classes, sealed trait hierarchies, etc.
 *
 * Typical usage will look like the following:
 *
 * {{{
 *   import io.circe._, io.circe.generic.semiauto._
 *
 *   case class Foo(i: Int, p: (String, Double))
 *
 *   object Foo {
 *     implicit val decodeFoo: Decoder[Foo] = deriveDecoder[Foo]
 *     implicit val encodeFoo: Encoder.AsObject[Foo] = deriveEncoder[Foo]
 *   }
 * }}}
 */
object semiauto {
  inline final def deriveDecoder[A](using inline A: Mirror.Of[A]): Decoder[A] = Decoder.derived[A]
  inline final def deriveEncoder[A](using inline A: Mirror.Of[A]): Encoder.AsObject[A] = Encoder.AsObject.derived[A]
  inline final def deriveCodec[A](using inline A: Mirror.Of[A]): Codec.AsObject[A] = Codec.AsObject.derived[A]
}
