package io.circe.generic.extras

import cats.Eq
import io.circe.literal._
import io.circe.tests.CirceSuite
import io.circe.{Decoder, Encoder}

class ConfiguredJsonCodecWithKeySuite extends CirceSuite {
  implicit val customConfig: Configuration =
    Configuration.default.withSnakeCaseKeys.withDefaults.withDiscriminator("type")

  /**
   * This nesting is necessary on 2.10 (possibly related to SI-7406).
   */
  object examples {
    @ConfiguredJsonCodec
    sealed trait ConfigExampleBase
    case class ConfigExampleFoo(thisIsAField: String, a: Int = 0, @Key("myField") b: Double) extends ConfigExampleBase

    object ConfigExampleFoo {
      implicit val eqConfigExampleFoo: Eq[ConfigExampleFoo] = Eq.fromUniversalEquals
    }

    object ConfigExampleBase {
      implicit val eqConfigExampleBase: Eq[ConfigExampleBase] = Eq.fromUniversalEquals
    }
  }

  import examples._

  "ConfiguredJsonCodec" should "support key annotation and configuration" in forAll { (f: String, b: Double) =>
    val foo: ConfigExampleBase = ConfigExampleFoo(f, 0, b)
    val json = json"""{ "type": "ConfigExampleFoo", "this_is_a_field": $f, "myField": $b}"""
    val expected = json"""{ "type": "ConfigExampleFoo", "this_is_a_field": $f, "a": 0, "myField": $b}"""

    assert(Encoder[ConfigExampleBase].apply(foo) === expected)
    assert(Decoder[ConfigExampleBase].decodeJson(json) === Right(foo))
  }
}
