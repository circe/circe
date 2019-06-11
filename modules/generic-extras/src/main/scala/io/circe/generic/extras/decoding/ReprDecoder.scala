package io.circe.generic.extras.decoding

import cats.data.Validated
import io.circe.{ ACursor, Decoder, HCursor }
import io.circe.Json.JNull
import io.circe.generic.extras.ConfigurableDeriver
import scala.collection.immutable.Map
import scala.language.experimental.macros
import shapeless.HNil

/**
 * A decoder for a generic representation of a case class or ADT.
 *
 * Note that users typically will not work with instances of this class, which
 * contains unsafe methods (specifically the two `configuredDecode` methods,
 * which allow passing in an untyped map of default field values).
 */
abstract class ReprDecoder[A] extends Decoder[A] {
  def configuredDecode(c: HCursor)(
    transformMemberNames: String => String,
    transformConstructorNames: String => String,
    defaults: Map[String, Any],
    discriminator: Option[String]
  ): Decoder.Result[A]

  def configuredDecodeAccumulating(c: HCursor)(
    transformMemberNames: String => String,
    transformConstructorNames: String => String,
    defaults: Map[String, Any],
    discriminator: Option[String]
  ): Decoder.AccumulatingResult[A]

  final protected[this] def orDefault[B](
    c: ACursor,
    decoder: Decoder[B],
    name: String,
    defaults: Map[String, Any]
  ): Decoder.Result[B] = {
    decoder.tryDecode(c) match {
      case r @ Right(_) if r ne Decoder.keyMissingNone            => r
      case l @ Left(_) if c.succeeded && !c.focus.contains(JNull) => l
      case r =>
        defaults.get(name) match {
          case Some(d: B @unchecked) => Right(d)
          case _                     => r
        }
    }
  }

  final protected[this] def orDefaultAccumulating[B](
    c: ACursor,
    decoder: Decoder[B],
    name: String,
    defaults: Map[String, Any]
  ): Decoder.AccumulatingResult[B] = {
    decoder.tryDecodeAccumulating(c) match {
      case r @ Validated.Valid(_) if r ne Decoder.keyMissingNoneAccumulating   => r
      case l @ Validated.Invalid(_) if c.succeeded && !c.focus.contains(JNull) => l
      case r =>
        defaults.get(name) match {
          case Some(d: B @unchecked) => Validated.valid(d)
          case _                     => r
        }
    }
  }

  final protected[this] def withDiscriminator[V](
    decode: Decoder[V],
    c: HCursor,
    name: String,
    discriminator: Option[String]
  ): Option[Decoder.Result[V]] = discriminator match {
    case None =>
      val result = c.downField(name)

      if (result.succeeded) Some(decode.tryDecode(result)) else None
    case Some(disc) =>
      c.get[String](disc) match {
        case Right(leafType) if leafType == name => Some(decode(c))
        case Right(_)                            => None
        case l @ Left(_)                         => Some(l.asInstanceOf[Decoder.Result[V]])
      }
  }

  final protected[this] def withDiscriminatorAccumulating[V](
    decode: Decoder[V],
    c: HCursor,
    name: String,
    discriminator: Option[String]
  ): Option[Decoder.AccumulatingResult[V]] = discriminator match {
    case None =>
      val result = c.downField(name)

      if (result.succeeded) Some(decode.tryDecodeAccumulating(result)) else None
    case Some(disc) =>
      c.get[String](disc) match {
        case Right(leafType) if leafType == name =>
          Some(decode.tryDecodeAccumulating(c))
        case Right(_)  => None
        case Left(err) => Some(Validated.invalidNel(err))
      }
  }

  final def apply(c: HCursor): Decoder.Result[A] =
    configuredDecode(c)(Predef.identity, Predef.identity, Map.empty, None)

  final override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
    configuredDecodeAccumulating(c)(Predef.identity, Predef.identity, Map.empty, None)
}

final object ReprDecoder {
  implicit def deriveReprDecoder[R]: ReprDecoder[R] = macro ConfigurableDeriver.deriveDecoder[R]

  val hnilReprDecoder: ReprDecoder[HNil] = new ReprDecoder[HNil] {
    def configuredDecode(c: HCursor)(
      transformMemberNames: String => String,
      transformConstructorNames: String => String,
      defaults: Map[String, Any],
      discriminator: Option[String]
    ): Decoder.Result[HNil] = Right(HNil)

    def configuredDecodeAccumulating(c: HCursor)(
      transformMemberNames: String => String,
      transformConstructorNames: String => String,
      defaults: Map[String, Any],
      discriminator: Option[String]
    ): Decoder.AccumulatingResult[HNil] = Validated.valid(HNil)
  }
}
