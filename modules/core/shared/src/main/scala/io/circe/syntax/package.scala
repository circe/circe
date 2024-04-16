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

/**
 * This package provides syntax via enrichment classes.
 */
package object syntax {
  implicit final class EncoderOps[A](private val value: A) extends AnyVal {
    final def asJson(implicit encoder: Encoder[A]): Json = encoder(value)
    final def asJsonObject(implicit encoder: Encoder.AsObject[A]): JsonObject =
      encoder.encodeObject(value)
  }
  implicit final class KeyOps[K](private val value: K) extends AnyVal {
    final def :=[A: Encoder](a: A)(implicit keyEncoder: KeyEncoder[K]): (String, Json) = (keyEncoder(value), a.asJson)
  }
}
