package io.circe

import io.circe.tests.CirceSuite
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Millis, Span }
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class PrinterWriterReuseSuite extends CirceSuite with ScalaFutures {
  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = Span(1000, Millis))

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration = PropertyCheckConfiguration(
    sizeRange = 1000
  )

  "Printer#reuseWriters" should "not change the behavior of pretty" in forAll { (values: List[Json]) =>
    val default = Printer.spaces4
    val reusing = default.copy(reuseWriters = true)
    val expected = values.map(default.pretty)

    whenReady(Future.traverse(values)(value => Future(reusing.pretty(value)))) { result =>
      assert(result === expected)
    }
  }
}
