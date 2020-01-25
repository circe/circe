package io.circe

import cats.kernel.instances.list._
import cats.kernel.instances.string._
import cats.kernel.instances.vector._
import cats.syntax.eq._
import io.circe.tests.PrinterSuite

trait SortedKeysSuite { this: PrinterSuite =>
  "Printer with sortKeys" should "sort the object keys (example)" in {
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

  "Printer with sortKeys" should "sort the object keys" in {
    forAll { (value: Map[String, List[Int]]) =>
      val printed = printer.print(implicitly[Encoder[Map[String, List[Int]]]].apply(value))
      val parsed = parser.parse(printed).toOption.flatMap(_.asObject).get
      val keys = parsed.keys.toVector
      assert(keys.sorted === keys)
    }
  }
}
