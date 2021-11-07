package io.circe

import cats.kernel.instances.list._
import cats.kernel.instances.string._
import cats.kernel.instances.vector._
import cats.syntax.eq._
import io.circe.tests.PrinterSuite
import org.scalacheck.Prop.forAll

trait SortedKeysSuite { this: PrinterSuite =>
  test("Printer with sortKeys should sort the object keys (example)") {
    val input = Json.obj(
      "one" -> Json.fromInt(1),
      "two" -> Json.fromInt(2),
      "three" -> Json.fromInt(3)
    )

    parser.parse(printer.print(input)).toOption.flatMap(_.asObject) match {
      case None => fail("Cannot parse result back to an object")
      case Some(output) =>
        assert(output.keys.toList === List("one", "three", "two"))
    }
  }

  property("Printer with sortKeys should sort the object keys") {
    forAll { (value: Map[String, List[Int]]) =>
      val printed = printer.print(implicitly[Encoder[Map[String, List[Int]]]].apply(value))
      val parsed = parser.parse(printed).toOption.flatMap(_.asObject).get
      val keys = parsed.keys.toVector
      assert(keys.sorted === keys)
    }
  }
}
