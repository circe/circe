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

import cats.laws.discipline.SerializableTests
import cats.kernel.laws.SerializableLaws
import io.circe.tests.CirceMunitSuite
import org.scalacheck.Prop

class SerializableSuite extends CirceMunitSuite {
  property("Json should be serializable") {
    Prop.forAll { (j: Json) =>
      SerializableLaws.serializable(j); ()
    }
  }

  property("HCursor should be serializable") {
    Prop.forAll { (j: Json) =>
      SerializableLaws.serializable(j.hcursor); ()
    }
  }

  checkAll("Decoder[Int]", SerializableTests.serializable(Decoder[Int]))
  checkAll("Encoder[Int]", SerializableTests.serializable(Encoder[Int]))

  checkAll(
    "Encoder.AsArray[List[String]]",
    SerializableTests.serializable(Encoder.AsArray[List[String]])
  )

  checkAll(
    "Encoder.AsObject[Map[String, Int]]",
    SerializableTests.serializable(Encoder.AsObject[Map[String, Int]])
  )

  checkAll("Parser", SerializableTests.serializable(parser.`package`))
  checkAll("Printer", SerializableTests.serializable(Printer.noSpaces))
}
