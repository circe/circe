package io.circe.tests

import cats.instances.either._
import cats.laws._
import cats.laws.discipline._
import io.circe.{ Json, Parser, ParsingFailure }
import org.scalacheck.{ Arbitrary, Prop }
import org.typelevel.discipline.Laws

case class ParserLaws(parser: Parser) {
  def parsingRoundTripNoSpaces(json: Json): IsEq[Either[ParsingFailure, Json]] =
    parser.parse(json.noSpaces) <-> Right(json)

  def parsingRoundTripSpaces(json: Json): IsEq[Either[ParsingFailure, Json]] =
    parser.parse(json.spaces2) <-> Right(json)
}

case class ParserTests(p: Parser) extends Laws {
  def laws: ParserLaws = ParserLaws(p)

  def parser(implicit arbitraryJson: Arbitrary[Json]): RuleSet = new DefaultRuleSet(
    name = "parser",
    parent = None,
    "roundTripWithoutSpaces" -> Prop.forAll { (json: Json) =>
      laws.parsingRoundTripNoSpaces(json)
    },
    "roundTripWithSpaces" -> Prop.forAll { (json: Json) =>
      laws.parsingRoundTripSpaces(json)
    }
  )
}
