package io.circe.generic.decoding

import io.circe.{ Decoder, HCursor }
import io.circe.generic.util.PatchWithOptions
import shapeless.{ HList, LabelledGeneric }
import shapeless.ops.function.FnFromProduct
import shapeless.ops.record.RemoveAll

private[circe] trait IncompleteDerivedDecoders {
  implicit final def decodeIncompleteCaseClass[C, F, P <: HList, A, T <: HList, R <: HList](implicit
    ffp: FnFromProduct.Aux[P => A, F],
    gen: LabelledGeneric.Aux[A, T],
    removeAll: RemoveAll.Aux[T, P, (P, R)],
    decoder: DerivedConfiguredDecoder[C, R]
  ): DerivedConfiguredDecoder[C, F] = new DerivedConfiguredDecoder[C, F] {
    final def apply(c: HCursor): Decoder.Result[F] =
      decoder(c).map(r => ffp(p => gen.from(removeAll.reinsert((p, r)))))
  }

  implicit final def decodeCaseClassPatch[C, A, R <: HList, O <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    patch: PatchWithOptions.Aux[R, O],
    decoder: DerivedConfiguredDecoder[C, O]
  ): DerivedConfiguredDecoder[C, A => A] = new DerivedConfiguredDecoder[C, A => A] {
    final def apply(c: HCursor): Decoder.Result[A => A] =
      decoder(c).map(o => a => gen.from(patch(gen.to(a), o)))
  }
}
