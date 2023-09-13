/*
 * Copyright 2023 circe
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
