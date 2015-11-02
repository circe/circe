package io.circe.tests

import io.circe.JsonNumber
import org.scalacheck.{ Arbitrary, Gen }

case class JsonNumberString(s: String) {
  def toJsonNumber: JsonNumber = JsonNumber.fromString(s).getOrElse(
    sys.error(
      s"An Arbitrary-provided JsonNumberString should always contain a valid JsonNumber; %s isn't."
    )
  )
}

object JsonNumberString {
  implicit val arbitraryJsonNumberString: Arbitrary[JsonNumberString] =
    Arbitrary(
      for {
        sign <- Gen.oneOf("", "-")
        number <- Gen.oneOf(
          Gen.const("0"),
          for {
            nonZero <- Gen.choose(1, 9).map(_.toString)
            rest <- Gen.numStr
          } yield s"$nonZero$rest"
        )
        frac <- Gen.oneOf(
          Gen.const(""),
          Gen.nonEmptyListOf(Gen.numChar).map(_.mkString).map("." + _)
        )
        exp <- Gen.oneOf(
          Gen.const(""),
          for {
            e <- Gen.oneOf("e", "E")
            s <- Gen.oneOf("", "+", "-")
            n <- Gen.nonEmptyListOf(Gen.numChar).map(_.mkString)
          } yield s"$e$s$n"
        )
      } yield JsonNumberString(s"$sign$number$frac$exp")
    )
}
