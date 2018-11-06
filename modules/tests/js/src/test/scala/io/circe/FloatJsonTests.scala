package io.circe

import io.circe.tests.CirceSuite

/**
 * On the JVM this trait contains tests that fail because of limitations on
 * Scala.js.
 */
trait FloatJsonTests { this: CirceSuite =>
}
