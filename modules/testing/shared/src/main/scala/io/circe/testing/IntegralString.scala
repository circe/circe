package io.circe.testing

import org.scalacheck.{ Arbitrary, Gen }

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
