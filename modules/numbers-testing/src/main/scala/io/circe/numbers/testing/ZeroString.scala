package io.circe.numbers.testing

import org.scalacheck.{ Arbitrary, Gen }

/**
 * A JSON number with value zero, represented as a string.
 */
final case class ZeroString(value: String) {
  final def isNegative: Boolean = value.charAt(0) == '-'
}

final object ZeroString {
  /**
   * The exponent will either be an integral number or some number of zeroes.
   */
  private[this] val exponentPart: Gen[String] = for {
    e <- Gen.oneOf("e", "E")
    value <- Gen.oneOf(
      IntegralString.arbitraryIntegralString.arbitrary.map(_.value),
      for {
        sign  <- Gen.oneOf("", "-")
        zeros <- Gen.chooseNum(1, 255).map("0" * _)
      } yield s"$sign$zeros"
    )
  } yield s"$e$value"

  implicit final val arbitraryZeroString: Arbitrary[ZeroString] = Arbitrary(
    for {
      sign       <- Gen.oneOf("", "-")
      fractional <- Gen.option(Gen.chooseNum(1, 256).map(count => "." + ("0" * count))).map(_.getOrElse(""))
      exponent   <- Gen.option(exponentPart).map(_.getOrElse(""))
    } yield ZeroString(s"${ sign }0$fractional$exponent")
  )
}
