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

package io.circe.derivation.auto

import io.circe.{ Codec, Decoder, Encoder, Json }
import io.circe.derivation.{ Configuration, ConfiguredCodec, ConfiguredDecoder, ConfiguredEncoder }
import io.circe.`export`.Exported
import scala.deriving.Mirror

inline given exportedDecoder[A](using
  inline m: Mirror.Of[A],
  conf: Configuration = Configuration.default
): Exported[Decoder[A]] =
  new Exported(ConfiguredDecoder.derived[A])
inline given exportedEncoder[A](using
  inline m: Mirror.Of[A],
  conf: Configuration = Configuration.default
): Exported[Encoder.AsObject[A]] =
  new Exported(ConfiguredEncoder.derived[A])
inline given exportedCodec[A](using
  inline m: Mirror.Of[A],
  conf: Configuration = Configuration.default
): Exported[Codec.AsObject[A]] =
  new Exported(ConfiguredCodec.derived[A])
