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

package io.circe.generic.codec

import io.circe.{ Codec, Decoder, HCursor, JsonObject }
import shapeless.{ LabelledGeneric, Lazy }

abstract class DerivedAsObjectCodec[A] extends Codec.AsObject[A]

object DerivedAsObjectCodec {
  implicit def deriveCodec[A, R](implicit
    gen: LabelledGeneric.Aux[A, R],
    codec: Lazy[ReprAsObjectCodec[R]]
  ): DerivedAsObjectCodec[A] = new DerivedAsObjectCodec[A] {
    final def apply(c: HCursor): Decoder.Result[A] = codec.value.apply(c) match {
      case Right(r)    => Right(gen.from(r))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A]]
    }
    override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
      codec.value.decodeAccumulating(c).map(gen.from)

    final def encodeObject(a: A): JsonObject = codec.value.encodeObject(gen.to(a))
  }
}
