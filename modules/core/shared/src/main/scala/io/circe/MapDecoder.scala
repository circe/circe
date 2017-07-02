package io.circe

import cats.data.{ NonEmptyList, Validated }
import scala.collection.Map
import scala.collection.mutable.Builder

private[circe] abstract class MapDecoder[K, V, M[K, V] <: Map[K, V]](
  decodeK: KeyDecoder[K],
  decodeV: Decoder[V]
) extends Decoder[M[K, V]] {
  private[this] val alwaysDecodeK = if (decodeK.isInstanceOf[KeyDecoder.AlwaysKeyDecoder[K]]) {
    decodeK.asInstanceOf[KeyDecoder.AlwaysKeyDecoder[K]]
  } else {
    null
  }

  protected def createBuilder(): Builder[(K, V), M[K, V]]

  final def apply(c: HCursor): Decoder.Result[M[K, V]] = c.fields match {
    case None => Left[DecodingFailure, M[K, V]](MapDecoder.failure(c))
    case Some(fields) =>
      val it = fields.iterator
      val builder = createBuilder()
      var failed: DecodingFailure = null

      while (failed.eq(null) && it.hasNext) {
        val field = it.next
        val atH = c.downField(field)

        if (alwaysDecodeK.ne(null)) {
          atH.as(decodeV) match {
            case Right(value) => builder += ((alwaysDecodeK.decodeSafe(field), value))
            case Left(error) => failed = error
          }
        } else {
          decodeK(field) match {
            case None => failed = MapDecoder.failure(atH)
            case Some(k) => atH.as(decodeV) match {
              case Right(value) => builder += ((k, value))
              case Left(error) => failed = error
            }
          }
        }
      }

      if (failed.eq(null)) Right(builder.result) else Left(failed)
  }

  final override def decodeAccumulating(c: HCursor): AccumulatingDecoder.Result[M[K, V]] = c.fields match {
    case None => Validated.invalidNel[DecodingFailure, M[K, V]](MapDecoder.failure(c))
    case Some(fields) =>
      val it = fields.iterator
      val builder = createBuilder()
      var failed = false
      val failures = List.newBuilder[DecodingFailure]

      while (it.hasNext) {
        val field = it.next
        val atH = c.downField(field)

        if (alwaysDecodeK.ne(null)) {
          decodeV.tryDecodeAccumulating(atH) match {
            case Validated.Valid(value) => if (!failed) builder += ((alwaysDecodeK.decodeSafe(field), value))
            case Validated.Invalid(es) =>
              failed = true
              failures += es.head
              failures ++= es.tail
          }
        } else {
          decodeK(field) match {
            case Some(k) =>
              decodeV.tryDecodeAccumulating(atH) match {
                case Validated.Valid(value) => if (!failed) builder += ((k, value))
                case Validated.Invalid(es) =>
                  failed = true
                  failures += es.head
                  failures ++= es.tail
              }
            case None =>
              failed = true
              failures += MapDecoder.failure(atH)
          }
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
