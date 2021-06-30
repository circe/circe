package io.circe.tests

import io.circe.testing.{ ArbitraryInstances, EqInstances }
import munit.DisciplineSuite

/**
 * An opinionated stack of traits to improve consistency and reduce boilerplate in circe tests.
 */
trait CirceMunitSuite extends DisciplineSuite with ArbitraryInstances with EqInstances with MissingInstances {

  protected def group(name: String)(thunk: => Unit): Unit = {
    val countBefore = munitTestsBuffer.size
    val _ = thunk
    val countAfter = munitTestsBuffer.size
    val countRegistered = countAfter - countBefore
    val registered = munitTestsBuffer.toList.drop(countBefore)
    (0 until countRegistered).foreach(_ => munitTestsBuffer.remove(countBefore))
    registered.foreach(t => munitTestsBuffer += t.withName(s"$name - ${t.name}"))
  }

  /**
   * Scala 3 implicit conversion for assertion-driven property tests.
   *
   * [[munit.ScalaCheckSuite.unitToProp]] is used by Scala 2 to implicitly convert
   * Unit bodies to Prop in Scala 2, but Scala 3 doesn't use it implicitly.
   */
  implicit val unitToPropConversion: Conversion[Unit, org.scalacheck.Prop] = unitToProp(_)

}
