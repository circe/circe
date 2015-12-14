package io.circe.internal

import cats.{ Id, Monad }
import io.circe.{ ACursor, Cursor, HCursor, Json }

/**
 * Represents a decoder that cannot fail.
 */
trait PerfectDecoder[A] extends AbstractDecoder[Id, A]

object PerfectDecoder {
  final val monadPerfectDecoder: Monad[PerfectDecoder] = new Monad[PerfectDecoder] {
    def pure[A](a: A): PerfectDecoder[A] = new PerfectDecoder[A] {
      def apply(c: HCursor): A = a
    }
    def flatMap[A, B](fa: PerfectDecoder[A])(f: A => PerfectDecoder[B]): PerfectDecoder[B] =
      new PerfectDecoder[B] {
        def apply(c: HCursor): B = f(fa(c))(c)
      }
  }

  implicit final val decodeJson: PerfectDecoder[Json] = new PerfectDecoder[Json] {
    def apply(c: HCursor): Json = c.focus
  }

  implicit final val decodeCursor: PerfectDecoder[Cursor] = new PerfectDecoder[Cursor] {
    def apply(c: HCursor): Cursor = c.cursor
  }

  implicit final val decodeHCursor: PerfectDecoder[HCursor] = new PerfectDecoder[HCursor] {
    def apply(c: HCursor): HCursor = c
  }

  implicit final val decodeACursor: PerfectDecoder[ACursor] = new PerfectDecoder[ACursor] {
    def apply(c: HCursor): ACursor = c.acursor
  }
}
