package io.circe.squants

import squants.information._
import io.circe.syntax._

import io.circe.tests.CirceSuite

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
        |  "readable" : "30000000000,0 B",
        |  "number" : 30000.0,
        |  "unit" : "MB",
        |  "name" : "Information"
        |}
      """.stripMargin.trim

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
      """.stripMargin.trim

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
      """.stripMargin.trim

    val decoding =  decode[Information](j.toString)

    assert(decoding.toOption.isDefined)
  }


  "tests with others squants data" should "decode and encode" in {

    import _root_.squants.space.LengthConversions._
    import _root_.squants.space.Length
    import _root_.squants.motion._
    import _root_.squants.time.TimeConversions._

    val length = 200.cm
    val lengthj = length.asJson.toString.stripMargin.trim


    val lengthAsserted =
      """
        |{
        |  "readable" : "2,0 m",
        |  "number" : 200.0,
        |  "unit" : "cm",
        |  "name" : "Length"
        |}
      """.stripMargin.trim

    assert(lengthAsserted.equals(lengthj))

    val lengthRebuild = decode[Length](lengthj).toOption

    assert(lengthRebuild.exists(_.equals(length)))

    val speed = length / 2.seconds
    val speedj = speed.asJson.toString.stripMargin.trim

    val speedAsserted = """
        |{
        |  "readable" : "1,0 m/s",
        |  "number" : 1.0,
        |  "unit" : "m/s",
        |  "name" : "Velocity"
        |}
      """.stripMargin.trim

    assert(speedAsserted.equals(speedj))
    val speedRebuild = decode[Velocity](speedAsserted).toOption
    assert(speedRebuild.exists(_.equals(speed)))



  }
}


