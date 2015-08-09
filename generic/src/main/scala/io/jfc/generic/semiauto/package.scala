package io.jfc.generic

import io.jfc.{ Decoder, ObjectEncoder }
import shapeless.LabelledGeneric

package object semiauto
  extends BaseInstances
  with LabelledInstances
  with HListInstances {
  object tuple extends TupleInstances
  object incomplete extends IncompleteInstances

  def deriveFor[A]: DerivationHelper[A] = new DerivationHelper[A]

  class DerivationHelper[A] {
    def encoder[R](implicit
      gen: LabelledGeneric.Aux[A, R],
      e: ObjectEncoder[R]
    ): ObjectEncoder[A] = new ObjectEncoder[A] {
      def encodeObject(a: A) = e.encodeObject(gen.to(a))
    }

    def decoder[R](implicit
      gen: LabelledGeneric.Aux[A, R],
      d: Decoder[R]
    ): Decoder[A] = d.map(gen.from)
  }
}
