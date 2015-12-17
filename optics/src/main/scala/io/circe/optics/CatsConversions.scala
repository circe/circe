package io.circe.optics

import cats.Applicative
import scalaz.{ Applicative => ApplicativeZ }
import scalaz.syntax.apply._

trait CatsConversions {
  def csApplicative[F[_]](implicit F: ApplicativeZ[F]): Applicative[F] =
    new Applicative[F] {
      def map[A, B](fa: F[A])(f: A => B): F[B] = F.map(fa)(f)
      def pure[A](x: A): F[A] = F.point[A](x)
      def ap[A, B](fa: F[A])(f: F[A => B]): F[B] = F.ap(fa)(f)
      def product[A, B](fa: F[A], fb: F[B]): F[(A, B)] = (fa |@| fb).tupled
    }
}
