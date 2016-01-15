package io.circe.generic

import algebra.Eq
import cats.data.{ NonEmptyList, Validated, Xor }
import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.auto._
import io.circe.generic.config._
import io.circe.literal._
import io.circe.parse._
import io.circe.syntax._
import io.circe.tests.{ CirceSuite, CodecTests, ConfiguredCodecTests }
import io.circe.tests.examples._
import org.scalacheck.{ Arbitrary, Gen }
import shapeless.{ CNil, Witness }, shapeless.labelled.{ FieldType, field }
import shapeless.test.illTyped

class AutoDerivedConfiguredSuite extends CirceSuite {
  
  checkAll("Codec[(Int, Foo)] with SnakeCaseKeys", ConfiguredCodecTests[(Int, Foo), SnakeCaseKeys].codec)
  checkAll("Codec[Qux[Int]] with SnakeCaseKeys", ConfiguredCodecTests[Qux[Int], SnakeCaseKeys].codec)
  checkAll("Codec[Foo] with SnakeCaseKeys", ConfiguredCodecTests[Foo, SnakeCaseKeys].codec)

  checkAll("Codec[(Int, Foo)] with TypeField", ConfiguredCodecTests[(Int, Foo), TypeField].codec)
  checkAll("Codec[Qux[Int]] with TypeField", ConfiguredCodecTests[Qux[Int], TypeField].codec)
  checkAll("Codec[Foo] with TypeField", ConfiguredCodecTests[Foo, TypeField].codec)

  checkAll("Codec[(Int, Foo)] with CaseObjectString", ConfiguredCodecTests[(Int, Foo), CaseObjectString].codec)
  checkAll("Codec[Qux[Int]] with CaseObjectString", ConfiguredCodecTests[Qux[Int], CaseObjectString].codec)
  checkAll("Codec[Foo] with CaseObjectString", ConfiguredCodecTests[Foo, CaseObjectString].codec)

  checkAll("Codec[(Int, Foo)] with UseDefaultValues", ConfiguredCodecTests[(Int, Foo), UseDefaultValues].codec)
  checkAll("Codec[Qux[Int]] with UseDefaultValues", ConfiguredCodecTests[Qux[Int], UseDefaultValues].codec)
  checkAll("Codec[Foo] with UseDefaultValues", ConfiguredCodecTests[Foo, UseDefaultValues].codec)

  checkAll(
    "Codec[(Int, Foo)] with multiple options",
    ConfiguredCodecTests[(Int, Foo), UseDefaultValues with SnakeCaseKeys with CaseObjectString with TypeField].codec
  )

  checkAll(
    "Codec[Qux[Int]] with multiple options",
    ConfiguredCodecTests[Qux[Int], UseDefaultValues with SnakeCaseKeys with CaseObjectString with TypeField].codec
  )

  checkAll(
    "Codec[Foo] with multiple options",
    ConfiguredCodecTests[Foo, UseDefaultValues with SnakeCaseKeys with CaseObjectString with TypeField].codec
  )

  test("ConfiguredDecoder[Foo, SnakeCaseKeys]") {
    check { (bam: Bam) =>
      val result = bam.asJsonConfigured[SnakeCaseKeys]
      val expected = json"""{ "w": ${ bam.w }, "this_is_a_double_field": ${ bam.thisIsADoubleField } }"""

      result === expected
    }
  }

  test("ConfiguredDecoder[Foo, TypeField]") {
    check { (foo: Foo) =>
      val result = foo.asJsonConfigured[TypeField]

      val expected = foo match {
        case Bar(i, s) => json"""{ "i": $i, "s": $s, "type": "Bar" }"""
        case Baz(xs) => json"""{ "Baz": $xs }"""
        case Bam(w, thisIsADoubleField) => json"""{ "w": $w, "thisIsADoubleField": $thisIsADoubleField }"""
        case Xuq => json"""{ "type": "Xuq" }"""
      }

      result === expected
    }
  }

  test("ConfiguredDecoder[Foo, CaseObjectString]") {
    assert((Xuq: Foo).asJsonConfigured[CaseObjectString] === Json.string("Xuq"))
  }

  test("ConfiguredDecoder[Foo, UseDefaultValues]") {
    check { (i: Int, maybeS: Option[String]) =>
      val json = maybeS.fold(json""""{ "i": $i }""")(s => json""""{ "i": $i, "s": $s }""")
      val expected = maybeS.fold(Bar(i))(s => Bar(i, s))

      expected === decodeConfigured[Bar, UseDefaultValues](json.noSpaces)
    }
  }
}
