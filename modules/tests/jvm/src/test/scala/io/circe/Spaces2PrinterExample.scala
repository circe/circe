package io.circe

import scala.io.Source
import scala.util.Random

trait Spaces2PrinterExample { this: Spaces2PrinterSuite =>
  val rand: Random = new Random(0L)

  val doc: Json = (0 to 150).foldLeft(Json.obj()) {
    case (acc, i) if i % 3 == 0 =>
      val count = rand.nextInt(10)
      val strings = List.fill(count)(Json.fromString(rand.nextString(count)))
      val doubles = List.fill(count)(Json.fromDouble(rand.nextDouble)).flatten

      Json.obj(i.toString -> acc, "data" -> Json.fromValues(strings ++ doubles))
    case (acc, i) if i % 3 == 1 => Json.obj(i.toString -> acc, "data" -> Json.True)
    case (acc, i) => Json.obj(i.toString -> acc)
  }

  val source = Source.fromInputStream(getClass.getResourceAsStream("/io/circe/spaces2-example.json"))
  val expected = source.mkString
  source.close()

  "Printer.spaces2" should "generate the expected output for the example doc" in {
    val printed = Printer.spaces2.pretty(doc) + "\n"

    assert(printed === expected)
  }
}
