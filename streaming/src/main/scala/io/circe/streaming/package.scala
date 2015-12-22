package io.circe

import _root_.jawn.{ AsyncParser, ParseException }
import cats.MonadError
import cats.data.Xor
import io.circe.jawn.CirceSupportParser
import io.iteratee.{ Enumeratee, Enumerator }

package object streaming {
  def stringParser[F[_]](implicit F: MonadError[F, Throwable]): Enumeratee[F, String, Json] =
    new ParsingEnumeratee[F, String] {
      protected[this] def parseWith(parser: AsyncParser[Json])(
        in: String
      ): Either[ParseException, Seq[Json]] = parser.absorb(in)(CirceSupportParser.facade)
    }

  def byteParser[F[_]](implicit F: MonadError[F, Throwable]): Enumeratee[F, Array[Byte], Json] =
    new ParsingEnumeratee[F, Array[Byte]] {
      protected[this] def parseWith(parser: AsyncParser[Json])(
        in: Array[Byte]
      ): Either[ParseException, Seq[Json]] = parser.absorb(in)(CirceSupportParser.facade)
    }

  def decoder[F[_], A](implicit
    F: MonadError[F, Throwable],
    decode: Decoder[A]
  ): Enumeratee[F, Json, A] =
    Enumeratee.flatMap(json =>
      decode(json.hcursor) match {
        case Xor.Left(df) => Enumerator.liftM(F.raiseError(df))
        case Xor.Right(a) => Enumerator.enumOne(a)
      }
    )
}
