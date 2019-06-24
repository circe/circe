package io.circe.generic.extras

import cats.kernel.Eq
import io.circe.{ Decoder, Encoder }
import io.circe.literal._
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Arbitrary.arbitrary

object ConfiguredJsonCodecWithKeySuite {
  implicit val customConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames.withDefaults.withDiscriminator("type").withSnakeCaseConstructorNames

  @ConfiguredJsonCodec
  sealed trait ConfigExampleBase
  case class ConfigExampleFoo(thisIsAField: String, a: Int = 0, @JsonKey("myField") b: Double) extends ConfigExampleBase

  object ConfigExampleFoo {
    implicit val eqConfigExampleFoo: Eq[ConfigExampleFoo] = Eq.fromUniversalEquals
    val genConfigExampleFoo: Gen[ConfigExampleFoo] = for {
      thisIsAField <- arbitrary[String]
      a <- arbitrary[Int]
      b <- arbitrary[Double]
    } yield ConfigExampleFoo(thisIsAField, a, b)
    implicit val arbitraryConfigExampleFoo: Arbitrary[ConfigExampleFoo] = Arbitrary(genConfigExampleFoo)
  }

  object ConfigExampleBase {
    implicit val eqConfigExampleBase: Eq[ConfigExampleBase] = Eq.fromUniversalEquals
    val genConfigExampleBase: Gen[ConfigExampleBase] =
      ConfigExampleFoo.genConfigExampleFoo
    implicit val arbitraryConfigExampleBase: Arbitrary[ConfigExampleBase] = Arbitrary(genConfigExampleBase)
  }
}

class ConfiguredJsonCodecWithKeySuite extends CirceSuite {
  import ConfiguredJsonCodecWithKeySuite._

  checkLaws("Codec[ConfigExampleBase]", CodecTests[ConfigExampleBase].codec)

  "ConfiguredJsonCodec" should "support key annotation and configuration" in forAll { (f: String, b: Double) =>
    val foo: ConfigExampleBase = ConfigExampleFoo(f, 0, b)
    val json = json"""{ "type": "config_example_foo", "this_is_a_field": $f, "myField": $b}"""
    val expected = json"""{ "type": "config_example_foo", "this_is_a_field": $f, "a": 0, "myField": $b}"""

    assert(Encoder[ConfigExampleBase].apply(foo) === expected)
    assert(Decoder[ConfigExampleBase].decodeJson(json) === Right(foo))
  }
}
