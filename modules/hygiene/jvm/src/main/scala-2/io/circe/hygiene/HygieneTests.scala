package io.circe.hygiene

import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.auto._
import io.circe.generic.semiauto._
import io.circe.literal._
import scala.StringContext
import shapeless.Witness

sealed trait Base

case class Foo(
  s: java.lang.String,
  i: scala.Int,
  o: scala.Option[scala.Double],
  b: scala.List[scala.Boolean]
) extends Base

case object Bar extends Base

/**
 * Compilation tests for macro hygiene.
 *
 * Fake definitions suggested by Jason Zaugg.
 */
object HygieneTests {
  val scala, Any, String, Unit = ()
  trait scala; trait Any; trait String; trait Unit

  val autoDerivedBaseEncoder: Encoder[Base] = Encoder[Base]
  val derivedBaseEncoder: Encoder[Base] = deriveEncoder[Base]

  val autoDerivedBaseDecoder: Decoder[Base] = Decoder[Base]
  val derivedBaseDecoder: Decoder[Base] = deriveDecoder[Base]

  val json: Json = json"""
    {
      "foo": {
        "s": "abcdef",
        "i": 10001,
        "o": 10.01,
        "b": [ true, false ]
      }
    }
  """

  val se = Encoder[Witness.`"foo"`.T]
  val sd = Decoder[Witness.`"foo"`.T]
  val be = Encoder[Witness.`true`.T]
  val db = Decoder[Witness.`true`.T]
  val de = Encoder[Witness.`1.0`.T]
  val dd = Decoder[Witness.`1.0`.T]
  val fe = Encoder[Witness.`1.0F`.T]
  val fd = Decoder[Witness.`1.0F`.T]
  val le = Encoder[Witness.`1L`.T]
  val ld = Decoder[Witness.`1L`.T]
  val ie = Encoder[Witness.`1`.T]
  val id = Decoder[Witness.`1`.T]
  val ce = Encoder[Witness.`'a'`.T]
  val cd = Decoder[Witness.`'a'`.T]
}
