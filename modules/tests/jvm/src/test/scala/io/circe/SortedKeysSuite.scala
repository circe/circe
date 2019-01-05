package io.circe
import io.circe.tests.PrinterSuite
import org.scalacheck.Prop
import org.scalatest.prop.Checkers

trait SortedKeysSuite { this: PrinterSuite =>
  "Printer with sortKeys" should "sort the object keys (example)" in {
    val input = Json.obj(
      "one" -> Json.fromInt(1),
      "two" -> Json.fromInt(2),
      "three" -> Json.fromInt(3)
    )

    parser.parse(printer.pretty(input)).toOption.flatMap(_.asObject) match {
      case None => fail("Cannot parse result back to an object")
      case Some(output) =>
        assert(output.keys.toList === List("one", "three", "two"))
    }
  }

  "Printer with sortKeys" should "sort the object keys" in {
    Checkers.check(Prop.forAll { value: Map[String, List[Int]] =>
      val printed = printer.pretty(implicitly[Encoder[Map[String, List[Int]]]].apply(value))
      val parsed = parser.parse(printed).toOption.flatMap(_.asObject).get
      val keys = parsed.keys.toVector
      keys.sorted === keys
    })
  }

}
