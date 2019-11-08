package io.circe.tests

import cats.instances._
import cats.syntax._
import io.circe.testing.{ ArbitraryInstances, EqInstances }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.scalacheck.{ Checkers, ScalaCheckDrivenPropertyChecks }
import org.typelevel.discipline.Laws
import scala.language.implicitConversions

/**
 * An opinionated stack of traits to improve consistency and reduce boilerplate in circe tests.
 */
trait CirceSuite
    extends AnyFlatSpec
    with ScalaCheckDrivenPropertyChecks
    with AllInstances
    with AllInstancesBinCompat0
    with AllInstancesBinCompat1
    with AllInstancesBinCompat2
    with AllInstancesBinCompat3
    with AllInstancesBinCompat4
    with AllInstancesBinCompat5
    with AllInstancesBinCompat6
    with AllSyntax
    with AllSyntaxBinCompat0
    with AllSyntaxBinCompat1
    with AllSyntaxBinCompat2
    with AllSyntaxBinCompat3
    with AllSyntaxBinCompat4
    with AllSyntaxBinCompat5
    with AllSyntaxBinCompat6
    with ArbitraryInstances
    with EqInstances
    with MissingInstances {
  override def convertToEqualizer[T](left: T): Equalizer[T] =
    sys.error("Intentionally ambiguous implicit for Equalizer")

  implicit def prioritizedCatsSyntaxEither[A, B](eab: Either[A, B]): EitherOps[A, B] = new EitherOps(eab)

  def checkLaws(name: String, ruleSet: Laws#RuleSet): Unit = ruleSet.all.properties.zipWithIndex.foreach {
    case ((id, prop), 0) => name should s"obey $id" in Checkers.check(prop)
    case ((id, prop), _) => it should s"obey $id" in Checkers.check(prop)
  }
}
