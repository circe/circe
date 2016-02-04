package io.circe.tests

import io.circe.JsonNumber
import org.scalacheck.{ Arbitrary, Gen }

case class JsonNumberString(s: String) {
  def toJsonNumber: JsonNumber = JsonNumber.unsafeDecimal(s)
}

object JsonNumberString {
  implicit val arbitraryJsonNumberString: Arbitrary[JsonNumberString] =
    Arbitrary(
      for {
        sign <- Gen.oneOf("", "-")
        integral <- Gen.oneOf(
          Gen.const("0"),
          for {
            nonZero <- Gen.choose(1, 9).map(_.toString)
            rest <- Gen.numStr
          } yield s"$nonZero$rest"
        )
        fractional <- Gen.oneOf(
          Gen.const(""),
          Gen.nonEmptyListOf(Gen.numChar).map(_.mkString).map("." + _)
        )
        exponent <- Gen.oneOf(
          Gen.const(""),
          for {
            e <- Gen.oneOf("e", "E")
            s <- Gen.oneOf("", "+", "-")
            n <- Gen.nonEmptyListOf(Gen.numChar).map(_.mkString)
          } yield s"$e$s$n"
        )
      } yield JsonNumberString(s"$sign$integral$fractional$exponent")
    )
}
