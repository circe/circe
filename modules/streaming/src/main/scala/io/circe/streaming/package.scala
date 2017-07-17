package io.circe

import _root_.jawn.{ AsyncParser, ParseException }
import cats.{ ApplicativeError, MonadError }
import io.circe.jawn.CirceSupportParser
import io.iteratee.{ Enumeratee, Enumerator }

package object streaming {
  final def stringArrayParser[F[_]](implicit F: ApplicativeError[F, Throwable]): Enumeratee[F, String, Json] =
    stringParser(AsyncParser.UnwrapArray)

  final def stringStreamParser[F[_]](implicit F: ApplicativeError[F, Throwable]): Enumeratee[F, String, Json] =
    stringParser(AsyncParser.ValueStream)

  final def byteArrayParser[F[_]](implicit F: ApplicativeError[F, Throwable]): Enumeratee[F, Array[Byte], Json] =
    byteParser(AsyncParser.UnwrapArray)

  final def byteStreamParser[F[_]](implicit F: ApplicativeError[F, Throwable]): Enumeratee[F, Array[Byte], Json] =
    byteParser(AsyncParser.ValueStream)

  final def decoder[F[_], A](implicit F: MonadError[F, Throwable], decode: Decoder[A]): Enumeratee[F, Json, A] =
    Enumeratee.flatMap(json =>
      decode(json.hcursor) match {
        case Left(df) => Enumerator.liftM(F.raiseError(df))
        case Right(a) => Enumerator.enumOne(a)
      }
    )

  private def stringParser[F[_]](mode: AsyncParser.Mode)
                                (implicit F: ApplicativeError[F, Throwable]): Enumeratee[F, String, Json] = {
    new ParsingEnumeratee[F, String] {
      protected[this] final def parseWith(p: AsyncParser[Json])(in: String): Either[ParseException, Seq[Json]] =
        p.absorb(in)(CirceSupportParser.facade)

      protected[this] val parsingMode: AsyncParser.Mode = mode
    }
  }

  private def byteParser[F[_]](mode: AsyncParser.Mode)
                              (implicit F: ApplicativeError[F, Throwable]): Enumeratee[F, Array[Byte], Json] =
    new ParsingEnumeratee[F, Array[Byte]] {
      protected[this] final def parseWith(p: AsyncParser[Json])(in: Array[Byte]): Either[ParseException, Seq[Json]] =
        p.absorb(in)(CirceSupportParser.facade)

      protected[this] val parsingMode: AsyncParser.Mode = mode
    }
}
