package io.circe.tests

import org.scalacheck.Prop

trait PlatformSpecificInstances {

  /**
   * Scala 3 implicit conversion for assertion-driven property tests.
   *
   * [[munit.ScalaCheckSuite.unitToProp]] is used by Scala 2 to implicitly convert
   * Unit bodies to Prop in Scala 2, but Scala 3 doesn't use it implicitly.
   */
  implicit val unitToPropConversion: Conversion[Unit, Prop] = unitToProp(_)

}
