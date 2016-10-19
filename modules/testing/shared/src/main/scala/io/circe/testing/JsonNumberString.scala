package io.circe.testing

import org.scalacheck.{ Arbitrary, Gen }

/**
 * An arbitrary JSON number, represented as a string.
 */
final case class JsonNumberString(value: String)

final object JsonNumberString {
  implicit val arbitraryJsonNumberString: Arbitrary[JsonNumberString] = Arbitrary(
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
