package io.circe.fs2

import _root_.fs2.{ Chunk, Handle, Pipe, Pull, Stream }
import _root_.jawn.{ AsyncParser, ParseException }
import io.circe.{ Json, ParsingFailure }
import io.circe.jawn.CirceSupportParser

private[fs2] abstract class ParsingPipe[F[_], S] extends Pipe[F, S, Json] {
  protected[this] def parseWith(parser: AsyncParser[Json])(in: S): Either[ParseException, Seq[Json]]

  private[this] final def makeParser: AsyncParser[Json] = CirceSupportParser.async(mode = AsyncParser.UnwrapArray)

  private[this] final def doneOrLoop[A](p: AsyncParser[Json])(h: Handle[F, S]): Pull[F, Json, Unit] =
    h.receive1 {
      case (s, h) => parseWith(p)(s) match {
        case Left(error) =>
          Pull.fail(ParsingFailure(error.getMessage, error))
        case Right(js) =>
          Pull.output(Chunk.seq(js)) >> doneOrLoop(p)(h)
      }
    }

  final def apply(s: Stream[F, S]): Stream[F, Json] = s.pull(doneOrLoop(makeParser))
}
