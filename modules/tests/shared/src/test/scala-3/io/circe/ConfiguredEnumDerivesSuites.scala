package io.circe

import cats.kernel.Eq
import cats.kernel.instances.all.*
import cats.syntax.eq.*
import cats.data.Validated
import io.circe.{ Codec, Decoder, DecodingFailure, Encoder, Json }
import io.circe.CursorOp.DownField
import io.circe.testing.CodecTests
import io.circe.tests.CirceMunitSuite
import io.circe.derivation.*
import io.circe.syntax.*
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Prop.forAll

object ConfiguredEnumDerivesSuites:
  // "derives ConfiguredEnumCodec" is not here so we can change the configuration for the derivation in each test
  enum IntercardinalDirections:
    case NorthEast, SouthEast, SouthWest, NorthWest
  object IntercardinalDirections:
    given Eq[IntercardinalDirections] = Eq.fromUniversalEquals
    given Arbitrary[IntercardinalDirections] = Arbitrary(
      Gen.oneOf(
        Gen.const(IntercardinalDirections.NorthEast),
        Gen.const(IntercardinalDirections.SouthEast),
        Gen.const(IntercardinalDirections.SouthWest),
        Gen.const(IntercardinalDirections.NorthWest)
      )
    )
class ConfiguredEnumDerivesSuites extends CirceMunitSuite:
  import ConfiguredEnumDerivesSuites.*

  /* // compileErrors cannot handle scala.compiletime.error: https://github.com/scalameta/munit/issues/453
  test("ConfiguredEnum derivation must fail to compile for enums with non singleton cases") {
    val error = compileErrors("""
        object WithNonSingletonCase:
          given EnumConfiguration = EnumConfiguration.default
        enum WithNonSingletonCase derives ConfiguredEnumCodec:
          case SingletonCase
          case NonSingletonCase(field: Int)""")
    val expectedError = """  enum WithNonSingletonCase derives ConfiguredEnumCodec:
                          |                                    ^
                          |Enum "WithNonSingletonCase" contains non singleton case "NonSingletonCase"""".stripMargin
    assertNoDiff(error, expectedError)
  }*/

  {
    given Configuration = Configuration.default
    given Codec[IntercardinalDirections] = ConfiguredEnumCodec.derived
    checkAll("Codec[IntercardinalDirections] (default configuration)", CodecTests[IntercardinalDirections].codec)
  }

  test("Fail to decode if case name does not exist") {
    given Configuration = Configuration.default
    given Codec[IntercardinalDirections] = ConfiguredEnumCodec.derived
    val json = Json.fromString("NorthNorth")
    val failure = DecodingFailure("enum IntercardinalDirections does not contain case: NorthNorth", List())
    assert(Decoder[IntercardinalDirections].decodeJson(json) === Left(failure))
    assert(Decoder[IntercardinalDirections].decodeAccumulating(json.hcursor) === Validated.invalidNel(failure))
  }

  test("Configuration#transformConstructorNames should support constructor name transformation with snake_case") {
    given Configuration = Configuration.default.withSnakeCaseConstructorNames
    given Codec[IntercardinalDirections] = ConfiguredEnumCodec.derived

    val direction = IntercardinalDirections.NorthEast
    val json = Json.fromString("north_east")
    assert(summon[Encoder[IntercardinalDirections]].apply(direction) === json)
    assert(summon[Decoder[IntercardinalDirections]].decodeJson(json) === Right(direction))
  }
  test(
    "Configuration#transformConstructorNames should support constructor name transformation with SCREAMING_SNAKE_CASE"
  ) {
    given Configuration = Configuration.default.withScreamingSnakeCaseConstructorNames
    given Codec[IntercardinalDirections] = ConfiguredEnumCodec.derived

    val direction = IntercardinalDirections.SouthEast
    val json = Json.fromString("SOUTH_EAST")
    assert(summon[Encoder[IntercardinalDirections]].apply(direction) === json)
    assert(summon[Decoder[IntercardinalDirections]].decodeJson(json) === Right(direction))
  }
  test("Configuration#transformConstructorNames should support constructor name transformation with kebab-case") {
    given Configuration = Configuration.default.withKebabCaseConstructorNames
    given Codec[IntercardinalDirections] = ConfiguredEnumCodec.derived

    val direction = IntercardinalDirections.SouthWest
    val json = Json.fromString("south-west")
    assert(summon[Encoder[IntercardinalDirections]].apply(direction) === json)
    assert(summon[Decoder[IntercardinalDirections]].decodeJson(json) === Right(direction))
  }
