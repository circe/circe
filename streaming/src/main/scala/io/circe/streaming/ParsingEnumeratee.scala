package io.circe.streaming

import _root_.jawn.{ AsyncParser, ParseException }
import cats.MonadError
import cats.std.either._
import cats.std.vector._
import cats.syntax.traverse._
import io.circe.{ Json, ParsingFailure }
import io.circe.jawn.CirceSupportParser
import io.iteratee.internal.Step
import io.iteratee.{ Enumeratee, Iteratee }

private[streaming] abstract class ParsingEnumeratee[F[_], S](implicit F: MonadError[F, Throwable])
  extends Enumeratee[F, S, Json] {
  protected[this] def parseWith(parser: AsyncParser[Json])(in: S): Either[ParseException, Seq[Json]]

  private[this] final def makeParser: AsyncParser[Json] =
    CirceSupportParser.async(mode = AsyncParser.UnwrapArray)

  private[this] final def feedStep[A](step: Step[F, Json, A], js: Seq[Json]): F[Step[F, Json, A]] =
    js match {
      case Seq() => F.pure(step)
      case Seq(e) => step.feedEl(e)
      case h1 +: h2 +: t => step.feedChunk(h1, h2, t.toVector)
    }

  private[this] final def stepWith[A](parser: AsyncParser[Json])
    (step: Step[F, Json, A]): Step[F, S, Step[F, Json, A]] =
      new Step.Cont[F, S, Step[F, Json, A]] {
        final def end: F[Step.Ended[F, S, Step[F, Json, A]]] =
          parser.finish()(CirceSupportParser.facade) match {
            case Left(error) => F.raiseError(ParsingFailure(error.getMessage, error))
            case Right(js) => F.map(feedStep(step, js))(Step.ended[F, S, Step[F, Json, A]])
          }
        final def onEl(e: S): F[Step[F, S, Step[F, Json, A]]] = parseWith(parser)(e) match {
          case Left(error) => F.raiseError(ParsingFailure(error.getMessage, error))
          case Right(js) => F.map(feedStep(step, js))(doneOrLoop[A](parser))
        }
        final def onChunk(h1: S, h2: S, t: Vector[S]): F[Step[F, S, Step[F, Json, A]]] =
          (h1 +: h2 +: t).traverseU(parseWith(parser)) match {
            case Left(error) => F.raiseError(ParsingFailure(error.getMessage, error))
            case Right(js) => F.map(feedStep(step, js.flatten))(doneOrLoop[A](parser))
          }
      }

  private[this] final def doneOrLoop[A](parser: AsyncParser[Json])
    (step: Step[F, Json, A]): Step[F, S, Step[F, Json, A]] =
      if (step.isDone) Step.done(step) else stepWith(parser)(step)

  final def apply[A](step: Step[F, Json, A]): F[Step[F, S, Step[F, Json, A]]] =
    F.pure(doneOrLoop(makeParser)(step))
}
