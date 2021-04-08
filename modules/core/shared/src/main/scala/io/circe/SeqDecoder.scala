package io.circe

import cats.data.{ NonEmptyList, Validated }
import scala.collection.mutable.Builder

private[circe] abstract class SeqDecoder[A, C[_]](decodeA: Decoder[A]) extends Decoder[C[A]] {
  protected def createBuilder(): Builder[A, C[A]]

  def apply(c: HCursor): Decoder.Result[C[A]] = {
    val values = c.value.asArray
    if (values.nonEmpty) {
      val jsonValues = values.get
      val builder = createBuilder()
      builder.sizeHint(jsonValues.size)
      var index = 0

      var failed: DecodingFailure = null
      jsonValues.foreach { value =>
        if (failed.eq(null)) {
          value.as(decodeA) match {
            case Left(e) =>
              // We expect a cursor that shifts N times to the right in an array in the tests,
              // so we must fix the current error
              var cursor = c.downArray
              var i = 0
              while (i < index) {
                cursor = cursor.right
                i += 1
              }
              val cursorHistory = cursor.history
              failed = e.copy(history = cursorHistory ++ e.history.drop(cursorHistory.size))
            case Right(a) =>
              builder += a
          }
        }
        index += 1
      }

      if (failed.eq(null)) Right(builder.result()) else Left(failed)
    } else {
      if (c.value.isArray) Right(createBuilder().result())
      else {
        Left(DecodingFailure("C[A]", c.history))
      }
    }
  }

  override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[C[A]] = {
    val values = c.value.asArray
    if (values.nonEmpty) {
      val jsonValues = values.get
      val builder = createBuilder()
      builder.sizeHint(jsonValues.size)
      var failed = false
      val failures = List.newBuilder[DecodingFailure]
      var index = 0

      jsonValues.foreach { value =>
        decodeA.decodeAccumulating(value.hcursor) match {
          case Validated.Invalid(es) =>
            failed = true
            // We expect a cursor that shifts N times to the right in an array in the tests,
            // so we must fix the current errors
            val fixedErrors = es.map { e =>
              var cursor = c.downArray
              var i = 0
              while (i < index) {
                cursor = cursor.right
                i += 1
              }
              val cursorHistory = cursor.history
              e.copy(history = cursorHistory ++ e.history.drop(cursorHistory.size))
            }
            failures += fixedErrors.head
            failures ++= fixedErrors.tail
          case Validated.Valid(a) =>
            if (!failed) {
              builder += a
            }
        }
        index += 1
      }

      if (!failed) Validated.valid(builder.result())
      else {
        failures.result() match {
          case h :: t => Validated.invalid(NonEmptyList(h, t))
          case Nil    => Validated.valid(builder.result())
        }
      }
    } else {
      if (c.value.isArray) Validated.valid(createBuilder().result())
      else {
        Validated.invalidNel(DecodingFailure("C[A]", c.history))
      }
    }
  }
}
