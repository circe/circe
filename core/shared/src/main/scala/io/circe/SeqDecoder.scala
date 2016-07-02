package io.circe

import cats.data.{ NonEmptyList, Validated, Xor }
import scala.collection.generic.CanBuildFrom

private[circe] class SeqDecoder[A, C[_]](
  decodeA: Decoder[A],
  cbf: CanBuildFrom[Nothing, A, C[A]]
) extends Decoder[C[A]] {
  final def apply(c: HCursor): Decoder.Result[C[A]] = {
    var current = c.downArray

    if (current.succeeded) {
      val builder = cbf.apply
      var failed: DecodingFailure = null

      while (failed.eq(null) && current.succeeded) {
        decodeA(current.any) match {
          case Xor.Left(e) => failed = e
          case Xor.Right(a) =>
            builder += a
            current = current.right
        }
      }

      if (failed.eq(null)) Xor.right(builder.result) else Xor.left(failed)
    } else {
      Xor.left(DecodingFailure("CanBuildFrom for A", c.history))
    }
  }

  override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[C[A]] = {
    var current = c.downArray

    if (current.succeeded) {
      val builder = cbf.apply
      var failed = false
      val failures = List.newBuilder[DecodingFailure]

      while (current.succeeded) {
        decodeA.decodeAccumulating(current.any) match {
          case Validated.Invalid(es) =>
            failed = true
            failures += es.head
            failures ++= es.tail
          case Validated.Valid(a) =>
            if (!failed) builder += a
            current = current.right
        }
      }

      failures.result match {
        case Nil => Validated.valid(builder.result)
        case h :: t => Validated.invalid(NonEmptyList(h, t))
      }
    } else {
      Validated.invalidNel(DecodingFailure("CanBuildFrom for A", c.history))
    }
  }
}
