package io.circe.streaming

import cats.MonadError
import cats.std.either._
import cats.std.vector._
import cats.syntax.traverse._
import io.circe.{ Json, ParsingFailure }
import io.circe.jawn.CirceSupportParser
import io.iteratee.{ Enumeratee, Input, InputFolder, Iteratee, Step, StepFolder }
import _root_.jawn.{ AsyncParser, ParseException }

private[streaming] abstract class ParsingEnumeratee[F[_], S](implicit F: MonadError[F, Throwable])
  extends Enumeratee[F, S, Json] {
  protected[this] def parseWith(parser: AsyncParser[Json])(in: S): Either[ParseException, Seq[Json]]

  private[this] final def makeParser: AsyncParser[Json] =
    CirceSupportParser.async(mode = AsyncParser.UnwrapArray)

  private[this] final def folder[A](parser: AsyncParser[Json])(
    k: Input[Json] => Iteratee[F, Json, A],
    in: Input[S]
  ): InputFolder[S, Outer[A]] = new InputFolder[S, Outer[A]] {
    def onEmpty: Outer[A] = Iteratee.cont(in => in.foldWith(folder(parser)(k, in)))
    def onEl(e: S): Outer[A] = parseWith(parser)(e) match {
      case Left(error) => Iteratee.fail[F, Throwable, S, Step[F, Json, A]](
        ParsingFailure(error.getMessage, error)
      )
      case Right(jsons) => k(Input.chunk(jsons.toVector)).advance(doneOrLoop(parser))
    }
    def onChunk(es: Vector[S]): Outer[A] = es.toVector.traverseU(parseWith(parser)) match {
      case Left(error) => Iteratee.fail[F, Throwable, S, Step[F, Json, A]](
        ParsingFailure(error.getMessage, error)
      )
      case Right(jsons) => k(Input.chunk(jsons.flatten)).advance(doneOrLoop(parser))
    }
    def onEnd = parser.finish()(CirceSupportParser.facade) match {
      case Left(error) => Iteratee.fail[F, Throwable, S, Step[F, Json, A]](
        ParsingFailure(error.getMessage, error)
      )
      case Right(jsons) => k(Input.chunk(jsons.toVector)).advance(toOuter)
    }
  }

  private[this] final def doneOrLoop[A](parser: AsyncParser[Json])(
    s: Step[F, Json, A]
  ): Outer[A] = s.foldWith(
    new StepFolder[F, Json, A, Outer[A]] {
      def onCont(k: Input[Json] => Iteratee[F, Json, A]): Outer[A] =
        Iteratee.cont(in => in.foldWith(folder(parser)(k, in)))
      def onDone(value: A, remaining: Input[Json]): Outer[A] = toOuter(s)
    }
  )

  final def apply[A](s: Step[F, Json, A]): Outer[A] = doneOrLoop(makeParser)(s)
}
