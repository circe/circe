package io.circe.testing

import cats.data.{Validated, ValidatedNel}
import cats.instances.either._
import cats.kernel.laws.SerializableLaws
import cats.laws._
import cats.laws.discipline._
import io.circe.{Error, Json, Parser, ParsingFailure, PrinterBuilder}
import org.scalacheck.{Arbitrary, Prop, Shrink}
import org.typelevel.discipline.Laws

case class ParserLaws[P <: Parser](parser: P) {
  def parsingRoundTrip[A](json: Json)(
    encode: Json => A, decode: P => (A => Either[ParsingFailure, Json])
  ): IsEq[Either[ParsingFailure, Json]] =
    decode(parser)(encode(json)) <-> Right(json)

  def decodingRoundTrip[A](json: Json)(
    encode: Json => A, decode: P => (A => Either[Error, Json])
  ): IsEq[Either[Error, Json]] =
    decode(parser)(encode(json)) <-> Right(json)

  def decodingAccumulatingRoundTrip[A](json: Json)(
    encode: Json => A, decode: P => (A => ValidatedNel[Error, Json])
  ): IsEq[ValidatedNel[Error, Json]] =
    decode(parser)(encode(json)) <-> Validated.valid(json)
}

case class ParserTests[P <: Parser](p: P) extends Laws {
  def laws: ParserLaws[P] = ParserLaws(p)

  def fromString(
    implicit arbitraryJson: Arbitrary[Json],
    shrinkJson: Shrink[Json],
    printerBuilder: PrinterBuilder
  ): RuleSet =
    fromFunction[String]("fromString")(
      identity, _.parse, _.decode[Json], _.decodeAccumulating[Json]
    )

  def fromFunction[A](name: String)(
    serialize: String => A,
    parse: P => A => Either[ParsingFailure, Json],
    decode: P => A => Either[Error, Json],
    decodeAccumulating: P => A => ValidatedNel[Error, Json]
  )(implicit arbitraryJson: Arbitrary[Json], shrinkJson: Shrink[Json], printerBuilder: PrinterBuilder): RuleSet =
    new DefaultRuleSet(
      name = s"parser[$name]",
      parent = None,
      "parsingRoundTripWithoutSpaces" -> Prop.forAll { (json: Json) =>
        laws.parsingRoundTrip[A](json)(json => serialize(printerBuilder.noSpaces.pretty(json)), parse)
      },
      "parsingRoundTripWithSpaces" -> Prop.forAll { (json: Json) =>
        laws.parsingRoundTrip[A](json)(json => serialize(printerBuilder.spaces2.pretty(json)), parse)
      },
      "decodingRoundTripWithoutSpaces" -> Prop.forAll { (json: Json) =>
        laws.decodingRoundTrip[A](json)(json => serialize(printerBuilder.noSpaces.pretty(json)), decode)
      },
      "decodingRoundTripWithSpaces" -> Prop.forAll { (json: Json) =>
        laws.decodingRoundTrip[A](json)(json => serialize(printerBuilder.spaces2.pretty(json)), decode)
      },
      "decodingAccumulatingRoundTripWithoutSpaces" -> Prop.forAll { (json: Json) =>
        laws.decodingAccumulatingRoundTrip[A](json)(json =>
          serialize(printerBuilder.noSpaces.pretty(json)), decodeAccumulating)
      },
      "decodingAccumulatingRoundTripWithSpaces" -> Prop.forAll { (json: Json) =>
        laws.decodingAccumulatingRoundTrip[A](json)(json =>
          serialize(printerBuilder.spaces2.pretty(json)), decodeAccumulating)
      },
      "parser serializability" -> SerializableLaws.serializable(p)
    )
}
