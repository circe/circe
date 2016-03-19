package io.circe

import _root_.jawn.{ AsyncParser, ParseException }
import cats.{ ApplicativeError, MonadError }
import cats.data.Xor
import io.circe.jawn.CirceSupportParser
import io.iteratee.{ Enumeratee, Enumerator }

package object streaming {
  final def stringParser[F[_]](implicit F: ApplicativeError[F, Throwable]): Enumeratee[F, String, Json] =
    new ParsingEnumeratee[F, String] {
      protected[this] final def parseWith(p: AsyncParser[Json])(in: String): Either[ParseException, Seq[Json]] =
        p.absorb(in)(CirceSupportParser.facade)
    }

  final def byteParser[F[_]](implicit F: ApplicativeError[F, Throwable]): Enumeratee[F, Array[Byte], Json] =
    new ParsingEnumeratee[F, Array[Byte]] {
      protected[this] final def parseWith(p: AsyncParser[Json])(in: Array[Byte]): Either[ParseException, Seq[Json]] =
        p.absorb(in)(CirceSupportParser.facade)
    }

  final def decoder[F[_], A](implicit F: MonadError[F, Throwable], decode: Decoder[A]): Enumeratee[F, Json, A] =
    Enumeratee.flatMap(json =>
      decode(json.hcursor) match {
        case Xor.Left(df) => Enumerator.liftM(F.raiseError(df))
        case Xor.Right(a) => Enumerator.enumOne(a)
      }
    )
}
