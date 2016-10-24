package io.circe.streaming

import _root_.jawn.{ AsyncParser, ParseException }
import cats.ApplicativeError
import cats.instances.either._
import cats.instances.vector._
import cats.syntax.traverse._
import io.circe.{ Json, ParsingFailure }
import io.circe.jawn.CirceSupportParser
import io.iteratee.{ Enumeratee, NonEmptyVector }
import io.iteratee.internal.Step

private[streaming] abstract class ParsingEnumeratee[F[_], S](implicit F: ApplicativeError[F, Throwable])
  extends Enumeratee[F, S, Json] {
  protected[this] def parseWith(parser: AsyncParser[Json])(in: S): Either[ParseException, Seq[Json]]

  private[this] final def makeParser: AsyncParser[Json] = CirceSupportParser.async(mode = AsyncParser.UnwrapArray)

  private[this] final def feedStep[A](step: Step[F, Json, A], js: Seq[Json]): F[Step[F, Json, A]] = js match {
    case Seq() => F.pure(step)
    case Seq(e) => step.feedEl(e)
    case h1 +: h2 +: t => step.feedChunk(h1, NonEmptyVector(h2, t.toVector))
  }

  private[this] final def loop[A](p: AsyncParser[Json])(step: Step[F, Json, A]): Step[F, S, Step[F, Json, A]] =
    new Step.Cont[F, S, Step[F, Json, A]] {
      final def run: F[Step[F, Json, A]] = p.finish()(CirceSupportParser.facade) match {
        case Left(error) => F.raiseError(ParsingFailure(error.getMessage, error))
        case Right(js) => feedStep(step, js)
      }
      final def feedEl(e: S): F[Step[F, S, Step[F, Json, A]]] = parseWith(p)(e) match {
        case Left(error) => F.raiseError(ParsingFailure(error.getMessage, error))
        case Right(js) => F.map(feedStep(step, js))(doneOrLoop[A](p))
      }
      final def feedChunk(h: S, t: NonEmptyVector[S]): F[Step[F, S, Step[F, Json, A]]] =
        (h +: t.toVector).traverseU(parseWith(p)) match {
          case Left(error) => F.raiseError(ParsingFailure(error.getMessage, error))
          case Right(js) => F.map(feedStep(step, js.flatten))(doneOrLoop[A](p))
        }
    }

  private[this] final def doneOrLoop[A](p: AsyncParser[Json])(step: Step[F, Json, A]): Step[F, S, Step[F, Json, A]] =
    if (step.isDone) Step.done(step) else loop(p)(step)

  final def apply[A](step: Step[F, Json, A]): F[Step[F, S, Step[F, Json, A]]] = F.pure(doneOrLoop(makeParser)(step))
}
