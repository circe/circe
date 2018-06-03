package io.circe

import scala.util.Try
import cats.syntax.either._
import io.circe.generic.auto._

import money.{Currency, Money}

/**
 * Provides codecs for [[https://github.com/lambdista/money money]] types.
 *
 *
 * E.g. with generic codecs
 * {{{
 *  val m = Money(100.0, Currency("GBP"))
 *
 *  m.asJson.noSpaces == """{"amount":100.0,"currency":"GBP"}"""
 * }}}
 *
 * @author Andreas Koestler
 */
package object monies {

  implicit val moneyEncoder = Encoder[Money]

  implicit val moneyDecoder = Decoder[Money]

  implicit val currencyEncoder: Encoder[Currency] = Encoder[Currency] { c =>
    Json.fromString(c.getCode)
  }

  implicit val currencyDecoder: Decoder[Currency] = Decoder.instance { c =>
    def safeDecode(s: String) =
      Try(Currency(s))
        .toEither
        .leftMap(err => DecodingFailure(s"Unsupported currency: $s", c.history))

    c.as[String].flatMap(safeDecode)
  }

}