package io.circe.tests

import io.circe.testing.{ ArbitraryInstances, EqInstances }
import munit.DisciplineSuite

/**
 * An opinionated stack of traits to improve consistency and reduce boilerplate in circe tests.
 */
trait CirceMunitSuite extends DisciplineSuite with ArbitraryInstances with EqInstances with MissingInstances
