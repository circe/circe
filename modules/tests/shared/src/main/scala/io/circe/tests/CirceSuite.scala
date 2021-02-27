package io.circe.tests

import cats.syntax._
import io.circe.testing.{ArbitraryInstances, EqInstances}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import org.typelevel.discipline.scalatest.FlatSpecDiscipline
import scala.language.implicitConversions

/**
 * An opinionated stack of traits to improve consistency and reduce boilerplate in circe tests.
 */
trait CirceSuite
    extends AnyFlatSpec
    with FlatSpecDiscipline
    with ScalaCheckDrivenPropertyChecks
    with ArbitraryInstances
    with EqInstances
    with MissingInstances {
  override def convertToEqualizer[T](left: T): Equalizer[T] =
    sys.error("Intentionally ambiguous implicit for Equalizer")

  implicit def prioritizedCatsSyntaxEither[A, B](eab: Either[A, B]): EitherOps[A, B] = new EitherOps(eab)
}
