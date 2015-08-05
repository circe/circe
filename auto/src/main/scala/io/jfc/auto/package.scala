package io.jfc

import shapeless._, shapeless.ops.function.FnFromProduct

package object auto extends GenericDecoding with GenericEncoding with LowPriorityAutoInstances {
  implicit def incompleteDecode[F, P <: HList, A, T <: HList, R <: HList](implicit
    ffp: FnFromProduct.Aux[P => A, F],
    gen: LabelledGeneric.Aux[A, T],
    complement: Complement.Aux[T, P, R],
    d: Decoder[R]
  ): Decoder[F] = d.map(r => ffp(p => gen.from(complement.insert(p, r))))

  /**
   * Return a [[io.jfc.Codec]].
   */
  implicit def tupleCodec[A, R <: HList](implicit
    isTuple: IsTuple[A],
    gen: Generic.Aux[A, R],
    d: Lazy[Decoder[R]],
    e: Lazy[Encoder[R]]
  ): Codec[A] = Codec.combined(d.value, e.value).imap(gen.from)(gen.to)

  implicit def adtCodec[A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    d: Lazy[Decoder[R]],
    e: Lazy[ObjectEncoder[R]]
  ): Codec[A] = Codec.combined(d.value, e.value).imap(gen.from)(gen.to)
}

package auto {
  trait LowPriorityAutoInstances {
    implicit def caseClassCodec[A, R <: HList](implicit
      gen: LabelledGeneric.Aux[A, R],
      d: Lazy[Decoder[R]],
      e: Lazy[Encoder[R]]
    ): Codec[A] = Codec.combined(d.value, e.value).imap(gen.from)(gen.to)
  }
}
