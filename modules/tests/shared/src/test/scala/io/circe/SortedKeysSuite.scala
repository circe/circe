package io.circe

import cats.kernel.instances.list._
import cats.kernel.instances.string._
import cats.syntax.eq._
import io.circe.tests.PrinterSuite
import org.scalacheck.Prop._

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
      val parsed = TestParser.parser.parse(printed).toOption.flatMap(_.asObject).get
      val keys = parsed.keys.toVector
      keys.sorted =? keys
    }
  }

  test("Sorting keys should handle \"\" consistently") {
    // From https://github.com/circe/circe/issues/1911
    val testMap: Map[String, List[Int]] = Map("4" -> Nil, "" -> Nil)

    val printed: String = printer.print(Encoder[Map[String, List[Int]]].apply(testMap))

    TestParser.parser.parse(printed) match {
      case Left(e) => fail(e.getLocalizedMessage, e)
      case Right(value) =>
        value.asObject.fold(fail(s"Expected object, but got ${value}.")) { value =>
          val keys: Vector[String] = value.keys.toVector
          assertEquals(keys.sorted, keys)
        }
    }
  }
}
