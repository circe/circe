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

import cats.kernel.Eq
import cats.kernel.laws.IsEq
import cats.kernel.laws.SerializableLaws
import cats.laws._
import cats.laws.discipline._
import io.circe.KeyDecoder
import io.circe.KeyEncoder
import org.scalacheck.Arbitrary
import org.scalacheck.Prop
import org.scalacheck.Shrink
import org.typelevel.discipline.Laws

trait KeyCodecLaws[A] {

  def decodeKey: KeyDecoder[A]
  def encodeKey: KeyEncoder[A]

  def keyCodecRoundTrip(a: A): IsEq[Option[A]] =
    decodeKey(encodeKey(a)) <-> Some(a)

}

object KeyCodecLaws {
  def apply[A: KeyDecoder: KeyEncoder]: KeyCodecLaws[A] = new KeyCodecLaws[A] {
    override val decodeKey: KeyDecoder[A] = KeyDecoder[A]
    override val encodeKey: KeyEncoder[A] = KeyEncoder[A]
  }
}

trait KeyCodecTests[A] extends Laws {
  def laws: KeyCodecLaws[A]

  def keyCodec(implicit
    arbitraryA: Arbitrary[A],
    shrinkA: Shrink[A],
    eqA: Eq[A],
    arbitraryString: Arbitrary[String],
    shrinkString: Shrink[String]
  ): RuleSet = new DefaultRuleSet(
    name = "keyCodec",
    parent = None,
    "roundTrip" -> Prop.forAll { (a: A) =>
      laws.keyCodecRoundTrip(a)
    },
    "keyDecoder serializability" -> SerializableLaws.serializable(laws.decodeKey),
    "keyEncoder serializability" -> SerializableLaws.serializable(laws.encodeKey)
  )

  def unserializableKeyCodec(implicit
    arbitraryA: Arbitrary[A],
    shrinkA: Shrink[A],
    eqA: Eq[A],
    arbitraryString: Arbitrary[String],
    shrinkString: Shrink[String]
  ): RuleSet = new DefaultRuleSet(
    name = "keyCodec",
    parent = None,
    "roundTrip" -> Prop.forAll { (a: A) =>
      laws.keyCodecRoundTrip(a)
    }
  )
}

object KeyCodecTests {
  def apply[A: KeyDecoder: KeyEncoder]: KeyCodecTests[A] = new KeyCodecTests[A] {
    override def laws: KeyCodecLaws[A] = KeyCodecLaws[A]
  }
}
