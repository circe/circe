package io.circe.tests

import cats.instances.AllInstances
import cats.syntax.{ AllSyntax, EitherOps }
import org.scalatest.FlatSpec
import org.scalatest.prop.{ Checkers, GeneratorDrivenPropertyChecks }
import org.typelevel.discipline.Laws
import scala.language.implicitConversions

/**
 * An opinionated stack of traits to improve consistency and reduce boilerplate in circe tests.
 */
trait CirceSuite extends FlatSpec with GeneratorDrivenPropertyChecks
  with AllInstances with AllSyntax
  with ArbitraryInstances with EqInstances with MissingInstances {

  override def convertToEqualizer[T](left: T): Equalizer[T] =
    sys.error("Intentionally ambiguous implicit for Equalizer")

  implicit def prioritizedCatsSyntaxEither[A, B](eab: Either[A, B]): EitherOps[A, B] = new EitherOps(eab)

  def checkLaws(name: String, ruleSet: Laws#RuleSet): Unit = ruleSet.all.properties.zipWithIndex.foreach {
    case ((id, prop), 0) => name should s"obey $id" in Checkers.check(prop)
    case ((id, prop), _) => it should s"obey $id" in Checkers.check(prop)
  }
}
