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

import cats.Contravariant

import java.io.Serializable
import java.net.URI
import java.util.UUID

/**
 * A type class that provides a conversion from a value of type `A` to a string.
 *
 * This type class will be used to create strings for JSON keys when encoding
 * `Map[A, ?]` instances as JSON.
 *
 * Note that if more than one value maps to the same string, the resulting JSON
 * object may have fewer fields than the original map.
 */
trait KeyEncoder[A] extends Serializable { self =>

  /**
   * Convert a key value to a string.
   */
  def apply(key: A): String

  /**
   * Construct an instance for type `B` from an instance for type `A`.
   */
  final def contramap[B](f: B => A): KeyEncoder[B] = new KeyEncoder[B] {
    final def apply(key: B): String = self(f(key))
  }
}

object KeyEncoder {
  @inline def apply[A](implicit A: KeyEncoder[A]): KeyEncoder[A] = A

  def instance[A](f: A => String): KeyEncoder[A] = new KeyEncoder[A] {
    def apply(key: A): String = f(key)
  }

  implicit val encodeKeyString: KeyEncoder[String] = new KeyEncoder[String] {
    final def apply(key: String): String = key
  }

  implicit val encodeKeySymbol: KeyEncoder[Symbol] = new KeyEncoder[Symbol] {
    final def apply(key: Symbol): String = key.name
  }

  implicit val encodeKeyUUID: KeyEncoder[UUID] = new KeyEncoder[UUID] {
    final def apply(key: UUID): String = key.toString
  }

  implicit val encodeKeyURI: KeyEncoder[URI] = new KeyEncoder[URI] {
    final def apply(key: URI): String = key.toString
  }

  implicit val encodeKeyByte: KeyEncoder[Byte] = new KeyEncoder[Byte] {
    final def apply(key: Byte): String = java.lang.Byte.toString(key)
  }

  implicit val encodeKeyShort: KeyEncoder[Short] = new KeyEncoder[Short] {
    final def apply(key: Short): String = java.lang.Short.toString(key)
  }

  implicit val encodeKeyInt: KeyEncoder[Int] = new KeyEncoder[Int] {
    final def apply(key: Int): String = java.lang.Integer.toString(key)
  }

  implicit val encodeKeyLong: KeyEncoder[Long] = new KeyEncoder[Long] {
    final def apply(key: Long): String = java.lang.Long.toString(key)
  }

  implicit val encodeKeyDouble: KeyEncoder[Double] = new KeyEncoder[Double] {
    final def apply(key: Double): String = java.lang.Double.toString(key)
  }

  implicit val keyEncoderContravariant: Contravariant[KeyEncoder] = new Contravariant[KeyEncoder] {
    final def contramap[A, B](e: KeyEncoder[A])(f: B => A): KeyEncoder[B] = e.contramap(f)
  }
}
