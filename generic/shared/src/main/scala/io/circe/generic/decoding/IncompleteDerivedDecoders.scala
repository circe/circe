package io.circe.generic.decoding

import io.circe.{ Decoder, HCursor }
import io.circe.generic.util.PatchWithOptions
import shapeless.{ HList, LabelledGeneric }
import shapeless.ops.function.FnFromProduct
import shapeless.ops.record.RemoveAll

private[circe] trait IncompleteDerivedDecoders extends LowPriorityIncompleteDerivedDecoders {
  implicit final def decodeIncompleteCaseClass[C, F, P <: HList, A, T <: HList, R <: HList](implicit
    ffp: FnFromProduct.Aux[P => A, F],
    gen: LabelledGeneric.Aux[A, T],
    removeAll: RemoveAll.Aux[T, P, (P, R)],
    decode: ConfiguredDerivedDecoder[C, R]
  ): ConfiguredDerivedDecoder[C, F] = new ConfiguredDerivedDecoder[C, F] {
    def apply(c: HCursor): Decoder.Result[F] =
      decode(c).map(r => ffp(p => gen.from(removeAll.reinsert((p, r)))))
  }

  implicit final def decodeCaseClassPatch[C, A, R <: HList, O <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    patch: PatchWithOptions.Aux[R, O],
    decode: ConfiguredDerivedDecoder[C, O]
  ): ConfiguredDerivedDecoder[C, A => A] = new ConfiguredDerivedDecoder[C, A => A] {
    def apply(c: HCursor): Decoder.Result[A => A] =
      decode(c).map(o => a => gen.from(patch(gen.to(a), o)))
  }
}

private[circe] trait LowPriorityIncompleteDerivedDecoders {
  implicit final
    def decodeIncompleteCaseClassUnconfigured[C, F, P <: HList, A, T <: HList, R <: HList](implicit
    ffp: FnFromProduct.Aux[P => A, F],
    gen: LabelledGeneric.Aux[A, T],
    removeAll: RemoveAll.Aux[T, P, (P, R)],
    decode: DerivedDecoder[R]
  ): ConfiguredDerivedDecoder[C, F] = new ConfiguredDerivedDecoder[C, F] {
    def apply(c: HCursor): Decoder.Result[F] =
      decode(c).map(r => ffp(p => gen.from(removeAll.reinsert((p, r)))))
  }

  implicit final def decodeCaseClassPatchUnconfigured[C, A, R <: HList, O <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    patch: PatchWithOptions.Aux[R, O],
    decode: DerivedDecoder[O]
  ): ConfiguredDerivedDecoder[C, A => A] = new ConfiguredDerivedDecoder[C, A => A] {
    def apply(c: HCursor): Decoder.Result[A => A] =
      decode(c).map(o => a => gen.from(patch(gen.to(a), o)))
  }
}
