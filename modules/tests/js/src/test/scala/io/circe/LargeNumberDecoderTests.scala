package io.circe

import io.circe.tests.CirceMunitSuite

/**
 * On the JVM this trait contains tests that fail because of bugs (or at least
 * limitations) on Scala.js.
 */
trait LargeNumberDecoderTests { this: CirceMunitSuite => }
