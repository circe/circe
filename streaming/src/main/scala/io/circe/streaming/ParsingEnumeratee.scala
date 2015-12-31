package io.circe.streaming

import _root_.jawn.{ AsyncParser, ParseException }
import cats.MonadError
import cats.std.either._
import cats.std.vector._
import cats.syntax.traverse._
import io.circe.{ Json, ParsingFailure }
import io.circe.jawn.CirceSupportParser
import io.iteratee.internal.{ Input, Step }
import io.iteratee.{ Enumeratee, Iteratee }

private[streaming] abstract class ParsingEnumeratee[F[_], S](implicit F: MonadError[F, Throwable])
  extends Enumeratee[F, S, Json] {
  protected[this] def parseWith(parser: AsyncParser[Json])(in: S): Either[ParseException, Seq[Json]]

  private[this] final def makeParser: AsyncParser[Json] =
    CirceSupportParser.async(mode = AsyncParser.UnwrapArray)

  private[this] final def folder[A](parser: AsyncParser[Json])(
    k: Input[Json] => F[Step[F, Json, A]],
    in: Input[S]
  ): Input.Folder[S, OuterF[A]] = new Input.Folder[S, OuterF[A]] {
    def onEmpty: OuterF[A] = F.pure(Step.cont(in => in.foldWith(folder(parser)(k, in))))
    def onEl(e: S): OuterF[A] = parseWith(parser)(e) match {
      case Left(error) => F.raiseError(
        ParsingFailure(error.getMessage, error)
      )
      case Right(jsons) => F.map(k(Input.chunk(jsons.toVector)))(doneOrLoop[A](parser))
    }
    def onChunk(es: Vector[S]): OuterF[A] = es.toVector.traverseU(parseWith(parser)) match {
      case Left(error) => F.raiseError(
        ParsingFailure(error.getMessage, error)
      )
      case Right(jsons) => F.map(k(Input.chunk(jsons.flatten)))(doneOrLoop(parser))
    }
    def onEnd: OuterF[A] = parser.finish()(CirceSupportParser.facade) match {
      case Left(error) => F.raiseError(
        ParsingFailure(error.getMessage, error)
      )
      case Right(jsons) => F.map(k(Input.chunk(jsons.toVector)))(s => Step.done(s, Input.empty))
    }
  }

  private[this] final def doneOrLoop[A](parser: AsyncParser[Json])(
    s: Step[F, Json, A]
  ): OuterS[A] = s.foldWith(
    new Step.Folder[F, Json, A, OuterS[A]] {
      def onCont(k: Input[Json] => F[Step[F, Json, A]]): OuterS[A] =
       Step.cont(in => in.foldWith(folder(parser)(k, in)))
      def onDone(value: A, remaining: Input[Json]): OuterS[A] = Step.done(s, Input.empty)
    }
  )

  final def apply[A](s: Step[F, Json, A]): OuterF[A] = F.pure(doneOrLoop(makeParser)(s))
}
