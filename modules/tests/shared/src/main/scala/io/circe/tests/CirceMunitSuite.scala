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

}
