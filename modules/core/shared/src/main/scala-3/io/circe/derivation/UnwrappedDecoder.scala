package io.circe.derivation

import io.circe._
import scala.deriving._
import scala.compiletime._

trait UnwrappedDecoder[A] extends Decoder[A]

object UnwrappedDecoder {
  implicit def allDecodersAreWrapped[A](implicit d: Decoder[A]): UnwrappedDecoder[A] =
    new UnwrappedDecoder[A] {
      override def apply(c: HCursor): Decoder.Result[A] = d(c)
    }
  inline final def derived[A](using inline mirror: Mirror.Of[A]): UnwrappedDecoder[A] =
    inline mirror match {
      case _: Mirror.SumOf[A] => error("UnwrappedDecoder only works for single field product types.")
      case m: Mirror.ProductOf[A] =>
        inline erasedValue[mirror.MirroredElemTypes] match {
          case _: EmptyTuple    => error("UnwrappedDecoder does not work for empty Product Types")
          case _: (_ *: _ *: _) => error("UnwrappedDecoder does not work for Product Types with size greater than 1")
          case _: (elem *: EmptyTuple) =>
            val innerDecoder: Decoder[elem] = summonInline[Decoder[elem]]
            new UnwrappedDecoder[A] {
              override def apply(c: HCursor): Decoder.Result[A] =
                innerDecoder(c).map { (e: elem) =>
                  m.fromProduct(e *: EmptyTuple)
                }
            }
        }
    }
}
