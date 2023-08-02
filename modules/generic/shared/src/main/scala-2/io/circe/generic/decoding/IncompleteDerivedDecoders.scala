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

package io.circe.generic.decoding

import io.circe.{ Decoder, HCursor }
import io.circe.generic.util.PatchWithOptions
import shapeless.{ HList, LabelledGeneric }
import shapeless.ops.function.FnFromProduct
import shapeless.ops.record.RemoveAll

private[circe] trait IncompleteDerivedDecoders {
  implicit final def decodeIncompleteCaseClass[F, P <: HList, A, T <: HList, R <: HList](implicit
    ffp: FnFromProduct.Aux[P => A, F],
    gen: LabelledGeneric.Aux[A, T],
    removeAll: RemoveAll.Aux[T, P, (P, R)],
    decode: ReprDecoder[R]
  ): DerivedDecoder[F] = new DerivedDecoder[F] {
    final def apply(c: HCursor): Decoder.Result[F] = decode(c) match {
      case Right(r)    => Right(ffp(p => gen.from(removeAll.reinsert((p, r)))))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[F]]
    }

    override final def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[F] =
      decode.decodeAccumulating(c).map(r => ffp(p => gen.from(removeAll.reinsert((p, r)))))
  }

  implicit final def decodeCaseClassPatch[A, R <: HList, O <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    patch: PatchWithOptions.Aux[R, O],
    decode: ReprDecoder[O]
  ): DerivedDecoder[A => A] = new DerivedDecoder[A => A] {
    final def apply(c: HCursor): Decoder.Result[A => A] = decode(c) match {
      case Right(o)    => Right(a => gen.from(patch(gen.to(a), o)))
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A => A]]
    }

    override final def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A => A] =
      decode.decodeAccumulating(c).map(o => a => gen.from(patch(gen.to(a), o)))
  }
}
