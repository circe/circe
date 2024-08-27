/*
 * Copyright 2024 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe.testing

import cats.data.Validated
import cats.data.ValidatedNel
import cats.kernel.laws.SerializableLaws
import cats.laws._
import cats.laws.discipline._
import io.circe.Error
import io.circe.Json
import io.circe.Parser
import io.circe.ParsingFailure
import org.scalacheck.Arbitrary
import org.scalacheck.Prop
import org.scalacheck.Shrink
import org.typelevel.discipline.Laws

case class ParserLaws[P <: Parser](parser: P) {
  def parsingRoundTrip[A](json: Json)(
    encode: Json => A,
    decode: P => (A => Either[ParsingFailure, Json])
  ): IsEq[Either[ParsingFailure, Json]] =
    decode(parser)(encode(json)) <-> Right(json)

  def decodingRoundTrip[A](json: Json)(
    encode: Json => A,
    decode: P => (A => Either[Error, Json])
  ): IsEq[Either[Error, Json]] =
    decode(parser)(encode(json)) <-> Right(json)

  def decodingAccumulatingRoundTrip[A](json: Json)(
    encode: Json => A,
    decode: P => (A => ValidatedNel[Error, Json])
  ): IsEq[ValidatedNel[Error, Json]] =
    decode(parser)(encode(json)) <-> Validated.valid(json)
}

case class ParserTests[P <: Parser](p: P) extends Laws {
  def laws: ParserLaws[P] = ParserLaws(p)

  def fromString(implicit arbitraryJson: Arbitrary[Json], shrinkJson: Shrink[Json]): RuleSet =
    fromFunction[String]("fromString")(
      identity,
      _.parse,
      _.decode[Json],
      _.decodeAccumulating[Json]
    )

  def fromFunction[A](name: String)(
    serialize: String => A,
    parse: P => A => Either[ParsingFailure, Json],
    decode: P => A => Either[Error, Json],
    decodeAccumulating: P => A => ValidatedNel[Error, Json]
  )(implicit arbitraryJson: Arbitrary[Json], shrinkJson: Shrink[Json]): RuleSet = new DefaultRuleSet(
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
    "decodingAccumulatingRoundTripWithoutSpaces" -> Prop.forAll { (json: Json) =>
      laws.decodingAccumulatingRoundTrip[A](json)(json => serialize(json.noSpaces), decodeAccumulating)
    },
    "decodingAccumulatingRoundTripWithSpaces" -> Prop.forAll { (json: Json) =>
      laws.decodingAccumulatingRoundTrip[A](json)(json => serialize(json.spaces2), decodeAccumulating)
    },
    "parser serializability" -> SerializableLaws.serializable(p)
  )
}
