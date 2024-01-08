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

case object Bar extends Base {

  val circeGenericHListBindingFori: _root_.scala.Any = null
  val circeGenericEncoderFori: (_root_.scala.Any => _root_.io.circe.Json) = x => null

  _root_.io.circe.JsonObject.fromIterable(_root_.scala.collection.immutable.Vector(
    if (circeGenericHListBindingFori == _root_.io.circe.Nullable.Undefined)
      _root_.scala.None
    else
      if (circeGenericHListBindingFori == _root_.io.circe.Nullable.Null)
        _root_.scala.Some(_root_.scala.Tuple2("i", _root_.io.circe.Json.Null))
      else
        _root_.scala.Some(_root_.scala.Tuple2("i", circeGenericEncoderFori(circeGenericHListBindingFori)))
  ).flatten)

}

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
