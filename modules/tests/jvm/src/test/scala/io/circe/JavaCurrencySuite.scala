package io.circe

import cats._
import io.circe.testing.CodecTests
import io.circe.tests.CirceMunitSuite
import java.util.Currency
import org.scalacheck._
import scala.collection.JavaConverters._

final class JavaCurrencySuite extends CirceMunitSuite {
  import JavaCurrencySuite._

  checkAll("Codec[Currency]", CodecTests[Currency].codec)
}

object JavaCurrencySuite {

  lazy val availableCurrencies: Set[Currency] =
    Currency.getAvailableCurrencies.asScala.toSet

  implicit lazy val arbitraryCurrency: Arbitrary[Currency] =
    Arbitrary(Gen.oneOf(availableCurrencies))

  // Orphans
  implicit private lazy val eqInstance: Eq[Currency] =
    Eq.fromUniversalEquals
}
