package io.circe.derivation

import io.circe._
import scala.deriving._
import scala.compiletime._

trait UnwrappedEncoder[A] extends Encoder[A]

object UnwrappedEncoder {
  implicit def allExistingEncodersAreWrapped[A](implicit e: Encoder[A]): UnwrappedEncoder[A] =
    new UnwrappedEncoder[A] {
      override def apply(a: A): Json = e(a)
    }
  inline final def derived[A](using inline mirror: Mirror.Of[A]): UnwrappedEncoder[A] =
    inline mirror match {
      case _: Mirror.SumOf[A] => error("UnwrappedEncoder only works for single field product types.")
      case m: Mirror.ProductOf[A] =>
        inline erasedValue[mirror.MirroredElemTypes] match {
          case _: EmptyTuple    => error("UnwrappedEncoder does not work for empty Product Types")
          case _: (_ *: _ *: _) => error("UnwrappedEncoder does not work for Product Types with size greater than 1")
          case _: (elem *: EmptyTuple) =>
            val innerEncoder: Encoder[elem] = summonInline[Encoder[elem]]
            new UnwrappedEncoder[A] {
              override def apply(a: A): Json = innerEncoder(
                a.asInstanceOf[Product].productElement(0).asInstanceOf[elem]
              )
            }
        }
    }
}
