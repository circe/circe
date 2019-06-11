package io.circe.tests.examples

import cats.kernel.Eq
import io.circe.{ Decoder, Encoder }
import org.scalacheck.Arbitrary

case class OptionalString(value: String) {
  def toOption: Option[String] = value match {
    case ""    => None
    case other => Some(other)
  }
}

object OptionalString {
  def fromOption(o: Option[String]): OptionalString =
    OptionalString(o.getOrElse(""))

  implicit val decodeOptionalString: Decoder[OptionalString] =
    Decoder[Option[String]].map(fromOption)

  implicit val encodeOptionalString: Encoder[OptionalString] =
    Encoder[Option[String]].contramap(_.toOption)

  implicit val eqOptionalString: Eq[OptionalString] = Eq.fromUniversalEquals

  implicit val arbitraryOptionalString: Arbitrary[OptionalString] =
    Arbitrary(Arbitrary.arbitrary[Option[String]].map(fromOption))
}

case class WrappedOptionalField(f: OptionalString)

object WrappedOptionalField {
  implicit val decodeWrappedOptionalField: Decoder[WrappedOptionalField] =
    Decoder.forProduct1("f")(WrappedOptionalField.apply)

  implicit val encodeWrappedOptionalField: Encoder[WrappedOptionalField] =
    Encoder.forProduct1("f")(_.f)

  implicit val eqWrappedOptionalField: Eq[WrappedOptionalField] =
    Eq.fromUniversalEquals

  implicit val arbitraryWrappedOptionalField: Arbitrary[WrappedOptionalField] =
    Arbitrary(Arbitrary.arbitrary[OptionalString].map(WrappedOptionalField(_)))
}
