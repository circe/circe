package io.jfc

import cats.data.Xor
import shapeless._, shapeless.ops.function.FnFromProduct

object AutoDecoding extends LabelledTypeClassCompanion[Decoder] {
  object typeClass extends LabelledTypeClass[Decoder] {
    def emptyCoproduct: Decoder[CNil] =
      Decoder.instance(c => Xor.left(DecodingFailure("CNil", c.history)))

    def coproduct[L, R <: Coproduct](
      name: String,
      cl: => Decoder[L],
      cr: => Decoder[R]
    ): Decoder[L :+: R] = Decoder.instance { c =>
      c.downField(name).focus.fold[Xor[DecodingFailure, L :+: R]](
        cr(c).map(Inr(_))
      ) { headJson =>
        headJson.as(cl).map(h => Inl(h))
      }
    }

    def emptyProduct: Decoder[HNil] = Decoder.instance(_ => Xor.right(HNil))

    def product[H, T <: HList](name: String, ch: Decoder[H], ct: Decoder[T]): Decoder[H :: T] =
      Decoder.instance { c =>
        for {
          head <- c.get(name)(ch)
          tail <- c.as(ct)
        } yield head :: tail
      }

    def project[F, G](instance: => Decoder[G], to: F => G, from: G => F): Decoder[F] =
      instance.map(from)
  }
}

object AutoEncoding extends LabelledTypeClassCompanion[Encoder] {
  object typeClass extends LabelledTypeClass[Encoder] {
    def emptyCoproduct: ObjectEncoder[CNil] =
      ObjectEncoder.instance(_ =>
        sys.error("JSON representation of CNil")
      )

    def coproduct[L, R <: Coproduct](
      name: String,
      cl: => Encoder[L],
      cr: => Encoder[R]
    ): Encoder[L :+: R] =
      ObjectEncoder.instance {
        case Inl(h) => JsonObject.singleton(name, cl(h))
        case Inr(t) => cr.asInstanceOf[ObjectEncoder[R]].encodeObject(t)
      }

    def emptyProduct: ObjectEncoder[HNil] = 
      ObjectEncoder.instance(_ => JsonObject.empty)

    def product[H, T <: HList](name: String, ch: Encoder[H], ct: Encoder[T]): ObjectEncoder[H :: T] =
      ObjectEncoder.instance {
        case h :: t => (name -> ch(h)) +: ct.asInstanceOf[ObjectEncoder[T]].encodeObject(t)
      }

    def project[F, G](instance: => Encoder[G], to: F => G, from: G => F): Encoder[F] =
      instance.contramap(to)
  }
}

package object auto {
  implicit def incompleteDecode[F, P <: HList, A, T <: HList, R <: HList, R2 <: HList](implicit
    ffp: FnFromProduct.Aux[P => A, F],
    gen: LabelledGeneric.Aux[A, T],
    d: Lazy[AutoDecoding.Wrap.Aux[R, R2]],
    complement: Complement.Aux[T, P, R2]
  ): Decoder[F] = d.value.unwrap.map(r => ffp(p => gen.from(complement.insert(p, r))))

  implicit def deriveDecoder[T, LKV](implicit
    lgen: LabelledGeneric.Aux[T, LKV],
    lwclkv: Lazy[AutoDecoding.Wrap[LKV]]
  ): Decoder.Secondary[T] = {
    import lwclkv.value._
    val to: T => V = (t: T) => unlabel(lgen.to(t))
    val from: V => T = (v: V) => lgen.from(label(v))
    new Decoder.Secondary(AutoDecoding.typeClass.project(unwrap, to, from))
  }

  implicit def deriveEncoder[T, LKV](implicit
    lgen: LabelledGeneric.Aux[T, LKV],
    lwclkv: Lazy[AutoEncoding.Wrap[LKV]]
  ): Encoder.Secondary[T] = {
    import lwclkv.value._
    val to: T => V = (t: T) => unlabel(lgen.to(t))
    val from: V => T = (v: V) => lgen.from(label(v))
    new Encoder.Secondary(AutoEncoding.typeClass.project(unwrap, to, from))
  }
}

/*extends GenericEncoding with LowPriorityAutoInstances {
  implicit def incompleteDecode[F, P <: HList, A, T <: HList, R <: HList](implicit
    ffp: FnFromProduct.Aux[P => A, F],
    gen: LabelledGeneric.Aux[A, T],
    complement: Complement.Aux[T, P, R],
    d: Decoder[R]
  ): Decoder[F] = d.map(r => ffp(p => gen.from(complement.insert(p, r))))

  implicit def decodeTuple[A, R <: HList](implicit
    isTuple: IsTuple[A],
    gen: Generic.Aux[A, R],
    d: Decoder[R]
  ): Decoder[A] = d.map(gen.from)

  implicit def encodeTuple[A, R <: HList](implicit
    isTuple: IsTuple[A],
    gen: Generic.Aux[A, R],
    e: Encoder[R]
  ): Encoder[A] = e.contramap(gen.to)

  implicit def decodeAdt[A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    d: Lazy[Decoder[R]]
  ): SecondaryDecoder[A] = new SecondaryDecoder(d.value.map(gen.from))

  implicit def encodeAdt[A, R <: Coproduct](implicit
    gen: LabelledGeneric.Aux[A, R],
    e: Lazy[ObjectEncoder[R]]
  ): SecondaryEncoder[A] = new SecondaryEncoder(e.value.contramap(gen.to))

}*/

package auto {
  trait LowPriorityAutoInstances {

    implicit def decodeCaseClass[A, R <: HList](implicit
      gen: LabelledGeneric.Aux[A, R],
      d: Lazy[Decoder[R]]
    ): Decoder.Secondary[A] = new Decoder.Secondary(d.value.map(gen.from))

    implicit def encodeCaseClass[A, R <: HList](implicit
      gen: LabelledGeneric.Aux[A, R],
      e: Lazy[Encoder[R]]
    ): Encoder.Secondary[A] = new Encoder.Secondary(e.value.contramap(gen.to))
    /*implicit def caseClassCodec[A, R <: HList](implicit
      gen: LabelledGeneric.Aux[A, R],
      d: Lazy[Decoder[R]],
      e: Lazy[Encoder[R]]
    ): Codec[A] = Codec.combined(d.value, e.value).imap(gen.from)(gen.to)*/
  }
}
