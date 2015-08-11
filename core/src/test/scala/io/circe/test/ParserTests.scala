package io.circe.test

import algebra.Eq
import cats.data.Xor
import cats.laws._
import cats.laws.discipline._
import io.circe.{ Json, ParsingFailure, Parser }
import org.scalacheck.Arbitrary
import org.scalacheck.Prop
import org.typelevel.discipline.Laws

case class ParserLaws(parser: Parser) {
  def parsingRoundTripNoSpaces(json: Json): IsEq[Xor[ParsingFailure, Json]] =
    parser.parse(json.noSpaces) <-> Xor.right(json)

  def parsingRoundTripSpaces(json: Json): IsEq[Xor[ParsingFailure, Json]] =
    parser.parse(json.spaces2) <-> Xor.right(json)
}

case class ParserTests(p: Parser) extends Laws {
  def laws: ParserLaws = ParserLaws(p)

  def parser(implicit arbitraryJson: Arbitrary[Json]): RuleSet = new DefaultRuleSet(
    name = "parser",
    parent = None,
    "roundTripWithoutSpaces" -> Prop.forAll(
      (json: Json) => isEqToProp(laws.parsingRoundTripNoSpaces(json))
    ),
    "roundTripWithSpaces" -> Prop.forAll(
      (json: Json) => isEqToProp(laws.parsingRoundTripSpaces(json))
    )
  )
}
