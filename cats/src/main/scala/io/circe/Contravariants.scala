package io.circe

import cats.functor.Contravariant

trait Contravariants {
  /**
   * @group Instances
   */
  implicit val contravariantEncode: Contravariant[Encoder] = new Contravariant[Encoder] {
    def contramap[A, B](e: Encoder[A])(f: B => A): Encoder[B] = e.contramap(f)
  }
}
