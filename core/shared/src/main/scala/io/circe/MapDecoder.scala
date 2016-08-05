package io.circe

import cats.data.{ NonEmptyList, Validated, Xor }
import scala.annotation.tailrec
import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.Builder

private[circe] final class MapDecoder[M[K, +V] <: Map[K, V], K, V](implicit
  dk: KeyDecoder[K],
  dv: Decoder[V],
  cbf: CanBuildFrom[Nothing, (K, V), M[K, V]]
) extends Decoder[M[K, V]] {
  def apply(c: HCursor): Decoder.Result[M[K, V]] = c.fields match {
    case None => Xor.left[DecodingFailure, M[K, V]](MapDecoder.failure(c))
    case Some(fields) =>
      val it = fields.iterator
      val builder = cbf.apply
      var failed: DecodingFailure = null

      while (failed.eq(null) && it.hasNext) {
        val field = it.next
        val atH = c.downField(field)

        atH.as(dv) match {
          case Xor.Left(error) =>
            failed = error
          case Xor.Right(value) => dk(field) match {
            case None =>
              failed = MapDecoder.failure(atH.any)
            case Some(k) =>
              builder += ((k, value))
          }
        }
      }

      if (failed.eq(null)) Xor.right(builder.result) else Xor.left(failed)
  }

  override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[M[K, V]] =
    c.fields match {
      case None => Validated.invalidNel[DecodingFailure, M[K, V]](MapDecoder.failure(c))
      case Some(fields) =>
        val it = fields.iterator
        val builder = cbf.apply
        var failed = false
        val failures = List.newBuilder[DecodingFailure]

        while (it.hasNext) {
          val field = it.next
          val atH = c.downField(field)

          dk(field) match {
            case Some(k) =>
              dv.tryDecodeAccumulating(atH) match {
                case Validated.Invalid(es) =>
                  failed = true
                  failures += es.head
                  failures ++= es.tail
                case Validated.Valid(value) =>
                  if (!failed) builder += ((k, value))
              }
            case None =>
              failed = true
              failures += MapDecoder.failure(atH.any)
          }
        }

        if (!failed) Validated.valid(builder.result) else {
          Validated.invalid(NonEmptyList.fromListUnsafe(failures.result))
        }
    }
}

private[circe] final object MapDecoder {
  def failure(c: HCursor): DecodingFailure = DecodingFailure("[K, V]Map[K, V]", c.history)
}
