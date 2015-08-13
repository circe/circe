package io.circe

import cats.Monad
import cats.data.Xor

trait Monads {
  implicit val monadDecode: Monad[Decoder] = new Monad[Decoder] {
    def pure[A](a: A): Decoder[A] = Decoder.instance(_ => Xor.right(a))
    def flatMap[A, B](fa: Decoder[A])(f: A => Decoder[B]): Decoder[B] = fa.flatMap(f)
  }
}
