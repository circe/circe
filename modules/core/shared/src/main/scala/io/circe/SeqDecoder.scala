package io.circe

import cats.data.{ NonEmptyList, Validated }
import scala.collection.mutable.Builder

private[circe] abstract class SeqDecoder[A, C[_]](decodeA: Decoder[A]) extends Decoder[C[A]] {
  protected def createBuilder(): Builder[A, C[A]]

  def apply(c: HCursor): Decoder.Result[C[A]] = {
    var current = c.downArray

    if (current.succeeded) {
      val builder = createBuilder()
      var failed: DecodingFailure = null

      while (failed.eq(null) && current.succeeded) {
        decodeA(current.asInstanceOf[HCursor]) match {
          case Left(e) => failed = e
          case Right(a) =>
            builder += a
            current = current.right
        }
      }

      if (failed.eq(null)) Right(builder.result) else Left(failed)
    } else {
      if (c.value.isArray) Right(createBuilder().result) else {
        Left(DecodingFailure("C[A]", c.history))
      }
    }
  }

  override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[C[A]] = {
    var current = c.downArray

    if (current.succeeded) {
      val builder = createBuilder()
      var failed = false
      val failures = List.newBuilder[DecodingFailure]

      while (current.succeeded) {
        decodeA.decodeAccumulating(current.asInstanceOf[HCursor]) match {
          case Validated.Invalid(es) =>
            failed = true
            failures += es.head
            failures ++= es.tail
          case Validated.Valid(a) =>
            if (!failed) builder += a
        }
        current = current.right
      }

      if (!failed) Validated.valid(builder.result) else {
        failures.result match {
          case h :: t => Validated.invalid(NonEmptyList(h, t))
          case Nil => Validated.valid(builder.result)
        }
      }
    } else {
      if (c.value.isArray) Validated.valid(createBuilder().result) else {
        Validated.invalidNel(DecodingFailure("C[A]", c.history))
      }
    }
  }
}
