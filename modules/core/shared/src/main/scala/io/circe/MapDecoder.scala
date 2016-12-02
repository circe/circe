package io.circe

import cats.data.{ NonEmptyList, Validated }
import scala.collection.generic.CanBuildFrom

private[circe] final class MapDecoder[M[K, +V] <: Map[K, V], K, V](implicit
  dk: KeyDecoder[K],
  dv: Decoder[V],
  cbf: CanBuildFrom[Nothing, (K, V), M[K, V]]
) extends Decoder[M[K, V]] {
  def apply(c: HCursor): Decoder.Result[M[K, V]] = c.fields match {
    case None => Left[DecodingFailure, M[K, V]](MapDecoder.failure(c))
    case Some(fields) =>
      val it = fields.iterator
      val builder = cbf.apply
      var failed: DecodingFailure = null

      while (failed.eq(null) && it.hasNext) {
        val field = it.next
        val atH = c.downField(field)

        dk(field) match {
          case None =>
            failed = MapDecoder.failure(atH)
          case Some(k) =>
            atH.as(dv) match {
              case Left(error) =>
                failed = error
              case Right(value) =>
                builder += ((k, value))
            }
        }
      }

      if (failed.eq(null)) Right(builder.result) else Left(failed)
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
              failures += MapDecoder.failure(atH)
          }
        }

        if (!failed) Validated.valid(builder.result) else {
          failures.result match {
            case h :: t => Validated.invalid(NonEmptyList(h, t))
            case Nil => Validated.valid(builder.result)
          }
        }
    }
}

private[circe] final object MapDecoder {
  def failure(c: ACursor): DecodingFailure = DecodingFailure("[K, V]Map[K, V]", c.history)
}
