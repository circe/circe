package io.circe.squants

import cats.kernel.Eq
import squants.information._
import io.circe._
import io.circe.syntax._
import io.circe.parser.parse
import io.circe.{Decoder, Encoder, Json, KeyDecoder, KeyEncoder}
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import io.circe.squants._
import org.scalacheck.{Arbitrary, Gen}
import shapeless.{Nat, Witness => W}
import io.circe.parser.decode


/**
  * Provides codecs tests for [[https://github.com/typelevel/squants squants]] types.
  *
  *
  * @author Quentin ADAM @waxzce
  */

class SquantsSuite extends CirceSuite {

  "an Information should " should "encode" in {
    val n = Information.primaryUnit.apply(1)

    val assertedResult =
      """
        |{
        |  "readable" : "1,0 B",
        |  "number" : 1.0,
        |  "unit" : "B",
        |  "name" : "Information"
        |}
      """.stripMargin.trim

    val j = n.asJson.toString.stripMargin.trim

    assert(assertedResult.equals(j))
  }

  "an Information should " should "use the best unit to encode readable" in {
    val n = Megabytes(30000)

    val assertedResult =
      """
        |{
        |  "readable" : "30000,0 MB",
        |  "number" : 30000.0,
        |  "unit" : "MB",
        |  "name" : "Information"
        |}
      """.stripMargin.trim

    //implicit val formatter = _root_.squants.experimental.formatter.Formatters.InformationMetricFormatter
    val j = n.asJson.toString.stripMargin.trim

    assert(assertedResult.equals(j))

  }

  "an Information should " should "use the provided formatter to encode readable" in {
    val n = Megabytes(30000)

    val assertedResult =
      """
        |{
        |  "readable" : "30,0 GB",
        |  "number" : 30000.0,
        |  "unit" : "MB",
        |  "name" : "Information"
        |}
      """.stripMargin.trim

    implicit val formatter = _root_.squants.experimental.formatter.Formatters.InformationMetricFormatter
    val j = n.asJson.toString.stripMargin.trim

    assert(assertedResult.equals(j))

  }


  "a have to fail without good data " should "fail" in {

    val j =
      """
        |{
        |  "readable" : "1.0 Byut",
        |  "number" : 1.0,
        |  "unit" : "Bfe",
        |  "name" : "Informdatison"
        |}
      """.stripMargin

    val decoding =  decode[Information](j.toString)

    assert(decoding.toOption.isEmpty)
  }


  "information without any readable field" should "decode using number and unit anyway" in {

    val j =
      """
        |{
        |  "number" : 1.0,
        |  "unit" : "B"
        |}
      """.stripMargin

    val decoding =  decode[Information](j.toString)

    assert(decoding.toOption.isDefined)
  }
}


