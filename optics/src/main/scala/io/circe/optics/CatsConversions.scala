package io.circe.optics

import cats.Applicative
import scalaz.{ Applicative => ApplicativeZ }

trait CatsConversions {
  def csApplicative[F[_]](F: ApplicativeZ[F]): Applicative[F] =
    new Applicative[F] {
      def pure[A](x: A): F[A] = F.point[A](x)
      def ap[A, B](fa: F[A])(f: F[A => B]): F[B] = F.ap(fa)(f)
    }
}

