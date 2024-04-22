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

package io.circe

import cats.kernel.Eq
import io.circe.parser.parse
import io.circe.syntax._
import io.circe.testing.CodecTests
import io.circe.tests.CirceMunitSuite
import org.scalacheck.{ Arbitrary, Gen }

class EnumerationSuite extends CirceMunitSuite {
  test("Decoder[Enumeration] should parse Scala Enumerations") {
    object WeekDay extends Enumeration {
      type WeekDay = Value
      val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value
    }

    val decoder = Decoder.decodeEnumeration(WeekDay)
    val Right(friday) = parse("\"Fri\"")
    assertEquals(decoder.apply(friday.hcursor), Right(WeekDay.Fri))
  }

  test("Decoder[Enumeration] should fail on unknown values in Scala Enumerations") {
    object WeekDay extends Enumeration {
      type WeekDay = Value
      val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value
    }

    val decoder = Decoder.decodeEnumeration(WeekDay)
    val Right(friday) = parse("\"Friday\"")

    val result = decoder.apply(friday.hcursor)
    assert(result.isLeft)
    assert(result.swap.exists(_.message.contains("Couldn't decode value 'Friday'. Allowed values:")))
  }

  test("Encoder[Enumeration] should write Scala Enumerations") {
    object WeekDay extends Enumeration {
      type WeekDay = Value
      val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value
    }

    implicit val encoder = Encoder.encodeEnumeration(WeekDay)
    val json = WeekDay.Fri.asJson
    val decoder = Decoder.decodeEnumeration(WeekDay)
    assertEquals(decoder.apply(json.hcursor), Right(WeekDay.Fri))
  }
}

class EnumerationCodecSuite extends CirceMunitSuite {
  object WeekDay extends Enumeration {
    type WeekDay = Value
    val Mon, Tue, Wed, Thu, Fri, Sat, Sun = Value

    implicit val arbitraryWeekDay: Arbitrary[WeekDay.WeekDay] = Arbitrary(
      Gen.oneOf(WeekDay.Mon, WeekDay.Tue, WeekDay.Wed, WeekDay.Thu, WeekDay.Fri, WeekDay.Sat, WeekDay.Sun)
    )
    implicit val eqWeekDay: Eq[WeekDay.WeekDay] = Eq.fromUniversalEquals
  }

  val decoder = Decoder.decodeEnumeration(WeekDay)
  val encoder = Encoder.encodeEnumeration(WeekDay)
  val codec = Codec.codecForEnumeration(WeekDay)

  checkAll("Codec[WeekDay.WeekDay]", CodecTests[WeekDay.WeekDay](decoder, encoder).unserializableCodec)
  checkAll("Codec[WeekDay.WeekDay] via Codec", CodecTests[WeekDay.WeekDay](codec, codec).unserializableCodec)
  checkAll(
    "Codec[WeekDay.WeekDay] via Decoder and Codec",
    CodecTests[WeekDay.WeekDay](decoder, codec).unserializableCodec
  )
  checkAll(
    "Codec[WeekDay.WeekDay] via Encoder and Codec",
    CodecTests[WeekDay.WeekDay](codec, encoder).unserializableCodec
  )
}
