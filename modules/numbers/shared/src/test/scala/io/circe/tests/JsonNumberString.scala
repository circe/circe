package io.circe.tests

import org.scalacheck.{ Arbitrary, Gen }

case class JsonNumberString(value: String)

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

case class IntegralString(value: String)

object IntegralString {
  implicit val arbitraryIntegralString: Arbitrary[IntegralString] =
    Arbitrary(
      for {
        sign    <- Gen.oneOf("", "-")
        nonZero <- Gen.choose(1, 9).map(_.toString)
        count   <- Gen.chooseNum(0, 24, 17, 18, 19)
        rest    <- Gen.buildableOfN[String, Char](count, Gen.numChar)
      } yield IntegralString(s"$sign$nonZero$rest")
    )
}
