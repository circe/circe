package io.circe.squants

import cats.kernel.Eq

import squants.information.Information



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

  "an Information should " should "encode and decode" in {
    val n = Information.primaryUnit.apply(1)

    val j = n.asJson
    println(j)

    val o = parse(j.toString).map(_.as[Information])

    println("decode  " + decode[Information](j.toString))

    println("----")
    assert(o.toOption.isDefined)
  }


  "a decoder without good data " should "fail" in {

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

    println("decode  " + decoding)
    println("decode  " + decoding.toOption)

    println("----")
    assert(decoding.toOption.isEmpty)
  }


  "information without any readable field" should "fulfil anyway" in {

    val j =
      """
        |{
        |  "number" : 1.0,
        |  "unit" : "B",
        |  "name" : "Informdatison"
        |}
      """.stripMargin

    val decoding =  decode[Information](j.toString)

    println("decode  " + decoding)
    println("decode  " + decoding.toOption)

    println("----")
    assert(decoding.toOption.isDefined)
  }
}


