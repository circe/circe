package io.circe

import io.circe.tests.CirceMunitSuite
import org.scalacheck.Prop
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._

class PrinterWriterReuseSuite extends CirceMunitSuite {
  implicit val ec: ExecutionContext = ExecutionContext.global

  override protected val scalaCheckTestParameters =
    super.scalaCheckTestParameters.withMaxSize(100)

  property("Printer#reuseWriters should not change the behavior of print")(reuseWritersProp)

  val reuseWritersProp: Prop = Prop.forAll { (values: List[Json]) =>
    val default = Printer.spaces4
    val reusing = default.copy(reuseWriters = true)
    val expected = values.map(default.print)

    val merged = Future.traverse(values)(value => Future(reusing.print(value)))
    assertEquals(Await.result(merged, 1.second), expected)
  }
}
