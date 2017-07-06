package io.circe.numbers.testing

import org.scalacheck.{ Arbitrary, Gen }

/**
 * An integral JSON number represented as a string.
 */
final case class IntegralString(value: String)

final object IntegralString {
  implicit val arbitraryIntegralString: Arbitrary[IntegralString] = Arbitrary(
    for {
      sign    <- Gen.oneOf("", "-")
      nonZero <- Gen.choose(1, 9).map(_.toString)
      /**
       * We want between 1 and 256 digits, with extra weight on the numbers of
       * digits around the size of `Long.MaxValue`.
       */
      count   <- Gen.chooseNum(0, 255, 17, 18, 19)
      rest    <- Gen.buildableOfN[String, Char](count, Gen.numChar)
    } yield IntegralString(s"$sign$nonZero$rest")
  )
}
