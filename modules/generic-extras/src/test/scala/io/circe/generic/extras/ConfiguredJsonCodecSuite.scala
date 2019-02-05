package io.circe.generic.extras

import cats.kernel.Eq
import io.circe.{ Decoder, Encoder, ObjectEncoder }
import io.circe.literal._
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import org.scalacheck.{ Arbitrary, Gen }

object ConfiguredJsonCodecSuite {
  implicit val customConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames.withDefaults.withDiscriminator("type").withSnakeCaseConstructorNames

  /**
   * This nesting is necessary on 2.10 (possibly related to SI-7406).
   */
  object localExamples {
    @ConfiguredJsonCodec
    sealed trait ConfigExampleBase
    case class ConfigExampleFoo(thisIsAField: String, a: Int = 0, b: Double) extends ConfigExampleBase

    object ConfigExampleFoo {
      implicit val eqConfigExampleFoo: Eq[ConfigExampleFoo] = Eq.fromUniversalEquals
      val genConfigExampleFoo: Gen[ConfigExampleFoo] = for {
        thisIsAField <- Arbitrary.arbitrary[String]
        a <- Arbitrary.arbitrary[Int]
        b <- Arbitrary.arbitrary[Double]
      } yield ConfigExampleFoo(thisIsAField, a, b)
      implicit val arbitraryConfigExampleFoo: Arbitrary[ConfigExampleFoo] = Arbitrary(genConfigExampleFoo)
    }

    object ConfigExampleBase {
      implicit val eqConfigExampleBase: Eq[ConfigExampleBase] = Eq.fromUniversalEquals
      val genConfigExampleBase: Gen[ConfigExampleBase] =
        ConfigExampleFoo.genConfigExampleFoo
      implicit val arbitraryConfigExampleBase: Arbitrary[ConfigExampleBase] = Arbitrary(genConfigExampleBase)
    }

    @ConfiguredJsonCodec private[circe] final case class AccessModifier(a: Int)

    private[circe] object AccessModifier {
      implicit def eqAccessModifier: Eq[AccessModifier] = Eq.fromUniversalEquals
      implicit def arbitraryAccessModifier: Arbitrary[AccessModifier] =
        Arbitrary(
          for {
            a <- Arbitrary.arbitrary[Int]
          } yield AccessModifier(a)
        )
    }
  }
}

class ConfiguredJsonCodecSuite extends CirceSuite {
  import ConfiguredJsonCodecSuite._, localExamples._

  checkLaws("Codec[ConfigExampleBase]", CodecTests[ConfigExampleBase].codec)
  checkLaws("Codec[AccessModifier]", CodecTests[AccessModifier].codec)

  "ConfiguredJsonCodec" should "support configuration" in forAll { (f: String, b: Double) =>
    val foo: ConfigExampleBase = ConfigExampleFoo(f, 0, b)
    val json = json"""{ "type": "config_example_foo", "this_is_a_field": $f, "b": $b}"""
    val expected = json"""{ "type": "config_example_foo", "this_is_a_field": $f, "a": 0, "b": $b}"""

    assert(Encoder[ConfigExampleBase].apply(foo) === expected)
    assert(Decoder[ConfigExampleBase].decodeJson(json) === Right(foo))
  }

  it should "allow only one, named argument set to true" in {
    // Can't supply both
    assertDoesNotCompile("@ConfiguredJsonCodec(encodeOnly = true, decodeOnly = true) case class X(a: Int)")
    // Must specify the argument name
    assertDoesNotCompile("@ConfiguredJsonCodec(true) case class X(a: Int)")
    // Can't specify false
    assertDoesNotCompile("@ConfiguredJsonCodec(encodeOnly = false) case class X(a: Int)")
  }

  "@ConfiguredJsonCodec(encodeOnly = true)" should "only provide Encoder instances" in {
    @ConfiguredJsonCodec(encodeOnly = true) case class CaseClassEncodeOnly(foo: String, bar: Int)
    Encoder[CaseClassEncodeOnly]
    ObjectEncoder[CaseClassEncodeOnly]
    assertDoesNotCompile("Decoder[CaseClassEncodeOnly]")
  }

  "@ConfiguredJsonCodec(decodeOnly = true)" should "provide Decoder instances" in {
    @ConfiguredJsonCodec(decodeOnly = true) case class CaseClassDecodeOnly(foo: String, bar: Int)
    Decoder[CaseClassDecodeOnly]
    assertDoesNotCompile("Encoder[CaseClassDecodeOnly]")
  }
}
