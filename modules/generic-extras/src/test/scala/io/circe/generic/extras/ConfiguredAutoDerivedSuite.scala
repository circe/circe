package io.circe.generic.extras

import cats.kernel.Eq
import io.circe.{ Decoder, Encoder, Json }
import io.circe.generic.extras.auto._
import io.circe.literal._
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import io.circe.tests.examples._

class ConfiguredAutoDerivedSuite extends CirceSuite {
  /**
   * This nesting is necessary on 2.10 (possibly related to SI-7406).
   */
  object examples {
    sealed trait ConfigExampleBase
    case class ConfigExampleFoo(thisIsAField: String, a: Int = 0, b: Double) extends ConfigExampleBase
    case object ConfigExampleBar extends ConfigExampleBase

    object ConfigExampleFoo {
      implicit val eqConfigExampleFoo: Eq[ConfigExampleFoo] = Eq.fromUniversalEquals
    }

    object ConfigExampleBase {
      implicit val eqConfigExampleBase: Eq[ConfigExampleBase] = Eq.fromUniversalEquals
    }
  }

  import examples._

  "Configuration#transformMemberNames" should "support member name transformation" in forAll { (f: String, a: Int, b: Double) =>
    implicit val snakeCaseConfig: Configuration = Configuration.default.withSnakeCaseMemberNames

    val foo: ConfigExampleFoo = ConfigExampleFoo(f, a, b)
    val json = json"""{ "this_is_a_field": $f, "a": $a, "b": $b}"""

    assert(Encoder[ConfigExampleFoo].apply(foo) === json)
    assert(Decoder[ConfigExampleFoo].decodeJson(json) === Right(foo))
  }

  "Configuration#useDefaults" should "support using default values during decoding" in {
    forAll { (f: String, b: Double) =>
      implicit val withDefaultsConfig: Configuration = Configuration.default.withDefaults

      val foo: ConfigExampleFoo = ConfigExampleFoo(f, 0, b)
      val json = json"""{ "thisIsAField": $f, "b": $b }"""
      val expected = json"""{ "thisIsAField": $f, "a": 0, "b": $b}"""

      assert(Encoder[ConfigExampleFoo].apply(foo) === expected)
      assert(Decoder[ConfigExampleFoo].decodeJson(json) === Right(foo))
    }
  }

  "Configuration#discriminator" should "support a field indicating constructor" in {
    forAll { (f: String, a: Int, b: Double) =>
      implicit val withDefaultsConfig: Configuration = Configuration.default.withDiscriminator("type")

      val foo: ConfigExampleBase = ConfigExampleFoo(f, a, b)
      val json = json"""{ "type": "ConfigExampleFoo", "thisIsAField": $f, "a": $a, "b": $b}"""

      assert(Encoder[ConfigExampleBase].apply(foo) === json)
      assert(Decoder[ConfigExampleBase].decodeJson(json) === Right(foo))
    }
  }

  "Configuration#transformConstructorNames" should "support constructor name transformation" in forAll { (f: String, a: Int, b: Double) =>
    implicit val snakeCaseConfig: Configuration = Configuration.default.withDiscriminator("type").withSnakeCaseConstructorNames

    val foo: ConfigExampleBase = ConfigExampleFoo(f, a, b)
    val json = json"""{ "type": "config_example_foo", "thisIsAField": $f, "a": $a, "b": $b}"""

    assert(Encoder[ConfigExampleBase].apply(foo) === json)
    assert(Decoder[ConfigExampleBase].decodeJson(json) === Right(foo))
  }

  "Configuration options" should "work together" in forAll { (f: String, b: Double) =>
    implicit val customConfig: Configuration =
      Configuration.default.withSnakeCaseMemberNames.withDefaults.withDiscriminator("type")

    val foo: ConfigExampleBase = ConfigExampleFoo(f, 0, b)
    val json = json"""{ "type": "ConfigExampleFoo", "this_is_a_field": $f, "b": $b}"""
    val expected = json"""{ "type": "ConfigExampleFoo", "this_is_a_field": $f, "a": 0, "b": $b}"""

    assert(Encoder[ConfigExampleBase].apply(foo) === expected)
    assert(Decoder[ConfigExampleBase].decodeJson(json) === Right(foo))
  }

  {
    import defaults._
    checkLaws("Codec[Tuple1[Int]]", CodecTests[Tuple1[Int]].codec)
    checkLaws("Codec[(Int, Int, Foo)]", CodecTests[(Int, Int, Foo)].codec)
    checkLaws("Codec[Qux[Int]]", CodecTests[Qux[Int]].codec)
    checkLaws("Codec[Foo]", CodecTests[Foo].codec)

    "Decoder[Int => Qux[String]]" should "decode partial JSON representations" in forAll { (i: Int, s: String, j: Int) =>
      val result = Json.obj(
        "a" -> Json.fromString(s),
        "j" -> Json.fromInt(j)
      ).as[Int => Qux[String]].map(_(i))

      assert(result === Right(Qux(i, s, j)))
    }
  }
}
