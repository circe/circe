package io.circe.testing

import org.scalacheck.{ Arbitrary, Gen }

/**
 * An integral string with an optional leading minus sign and between 1 and 25
 * digits (inclusive).
 */
final case class IntegralString(value: String)

final object IntegralString {
  implicit val arbitraryIntegralString: Arbitrary[IntegralString] = Arbitrary(
    for {
      sign    <- Gen.oneOf("", "-")
      nonZero <- Gen.choose(1, 9).map(_.toString)
      /**
       * We want between 1 and 25 digits, with extra weight on the numbers of
       * digits around the size of `Long.MaxValue`.
       */
      count   <- Gen.chooseNum(0, 24, 17, 18, 19)
      rest    <- Gen.buildableOfN[String, Char](count, Gen.numChar)
    } yield IntegralString(s"$sign$nonZero$rest")
  )
}
