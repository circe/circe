package io.circe.testing

import cats.data.{ Validated, ValidatedNel }
import cats.instances.either._
import cats.laws._
import cats.laws.discipline._
import io.circe.{ Error, Json, Parser, ParsingFailure }
import org.scalacheck.{ Arbitrary, Prop }
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

  def fromString(implicit arbitraryJson: Arbitrary[Json]): RuleSet =
    fromFunction[String]("fromString")(
      identity, _.parse, _.decode[Json], _.decodeAccumulating[Json]
    )

  def fromFunction[A](name: String)(
    serialize: String => A,
    parse: P => A => Either[ParsingFailure, Json],
    decode: P => A => Either[Error, Json],
    decodeAccumulating: P => A => ValidatedNel[Error, Json]
  )(implicit arbitraryJson: Arbitrary[Json]): RuleSet = new DefaultRuleSet(
    name = s"parser[$name]",
    parent = None,
    "parsingRoundTripWithoutSpaces" -> Prop.forAll { (json: Json) =>
      laws.parsingRoundTrip[A](json)(json => serialize(json.noSpaces), parse)
    },
    "parsingRoundTripWithSpaces" -> Prop.forAll { (json: Json) =>
      laws.parsingRoundTrip[A](json)(json => serialize(json.spaces2), parse)
    },
    "decodingRoundTripWithoutSpaces" -> Prop.forAll { (json: Json) =>
      laws.decodingRoundTrip[A](json)(json => serialize(json.noSpaces), decode)
    },
    "decodingRoundTripWithSpaces" -> Prop.forAll { (json: Json) =>
      laws.decodingRoundTrip[A](json)(json => serialize(json.spaces2), decode)
    },
    "decodingAccumualtingRoundTripWithoutSpaces" -> Prop.forAll { (json: Json) =>
      laws.decodingAccumulatingRoundTrip[A](json)(json =>
        serialize(json.noSpaces), decodeAccumulating)
    },
    "decodingAccumualtingRoundTripWithSpaces" -> Prop.forAll { (json: Json) =>
      laws.decodingAccumulatingRoundTrip[A](json)(json =>
        serialize(json.spaces2), decodeAccumulating)
    }
  )
}
