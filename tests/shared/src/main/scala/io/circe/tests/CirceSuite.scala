package io.circe.tests

import cats.std.AllInstances
import cats.syntax.AllSyntax
import org.scalatest.{ FunSuite, Matchers }
import org.typelevel.discipline.scalatest.Discipline

/**
 * An opinionated stack of traits to improve consistency and reduce boilerplate in circe tests.
 */
trait CirceSuite extends FunSuite with Matchers with Discipline with AllInstances with AllSyntax
  with ArbitraryInstances with EqInstances with MissingInstances {
  override def convertToEqualizer[T](left: T): Equalizer[T] =
    sys.error("Intentionally ambiguous implicit for Equalizer")
}
