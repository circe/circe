package io.circe.generic.decoding

import io.circe.{ Decoder, HCursor }
import io.circe.generic.util.{ Complement, PatchWithOptions }
import shapeless.{ HList, LabelledGeneric }, shapeless.ops.function.FnFromProduct

trait IncompleteDerivedDecoders {
  implicit def decodeIncompleteCaseClass[F, P <: HList, A, T <: HList, R <: HList](implicit
    ffp: FnFromProduct.Aux[P => A, F],
    gen: LabelledGeneric.Aux[A, T],
    complement: Complement.Aux[T, P, R],
    decode: DerivedDecoder[R]
  ): DerivedDecoder[F] = new DerivedDecoder[F] {
    def apply(c: HCursor): Decoder.Result[F] =
      decode(c).map(r => ffp(p => gen.from(complement.insert(p, r))))
  }

  implicit def decodeCaseClassPatch[A, R <: HList, O <: HList](implicit
    gen: LabelledGeneric.Aux[A, R],
    patch: PatchWithOptions.Aux[R, O],
    decode: DerivedDecoder[O]
  ): DerivedDecoder[A => A] = new DerivedDecoder[A => A] {
    def apply(c: HCursor): Decoder.Result[A => A] =
      decode(c).map(o => a => gen.from(patch(gen.to(a), o)))
  }
}
