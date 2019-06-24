package io.circe.generic.extras

import cats.data.Validated
import cats.kernel.Eq
import io.circe.{ Decoder, DecodingFailure, Encoder, Json }
import io.circe.CursorOp.DownField
import io.circe.generic.extras.auto._
import io.circe.literal._
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import io.circe.tests.examples._
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Arbitrary.arbitrary

object ConfiguredAutoDerivedSuite {

  sealed trait ConfigExampleBase
  case class ConfigExampleFoo(thisIsAField: String, a: Int = 0, b: Double) extends ConfigExampleBase
  case object ConfigExampleBar extends ConfigExampleBase

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
      Gen.oneOf(Gen.const(ConfigExampleBar), ConfigExampleFoo.genConfigExampleFoo)
    implicit val arbitraryConfigExampleBase: Arbitrary[ConfigExampleBase] = Arbitrary(genConfigExampleBase)
  }

  val genConfiguration: Gen[Configuration] = for {
    transformMemberNames <- arbitrary[String => String]
    transformConstructorNames <- arbitrary[String => String]
    useDefaults <- arbitrary[Boolean]
    discriminator <- arbitrary[Option[String]]
  } yield Configuration(transformMemberNames, transformConstructorNames, useDefaults, discriminator)
  implicit val arbitraryConfiguration: Arbitrary[Configuration] = Arbitrary(genConfiguration)
}

class ConfiguredAutoDerivedSuite extends CirceSuite {
  import ConfiguredAutoDerivedSuite._

  {
    implicit val config: Configuration = Configuration.default
    checkLaws("Codec[ConfigExampleBase] (default configuration)", CodecTests[ConfigExampleBase].codec)
  }

  "Configuration#transformMemberNames" should "support member name transformation using snake_case" in forAll {
    foo: ConfigExampleFoo =>
      implicit val snakeCaseConfig: Configuration = Configuration.default.withSnakeCaseMemberNames

      import foo._
      val json = json"""{ "this_is_a_field": $thisIsAField, "a": $a, "b": $b}"""

      assert(Encoder[ConfigExampleFoo].apply(foo) === json)
      assert(Decoder[ConfigExampleFoo].decodeJson(json) === Right(foo))
  }

  "Configuration#transformMemberNames" should "support member name transformation using kebab-case" in forAll {
    foo: ConfigExampleFoo =>
      implicit val kebabCaseConfig: Configuration = Configuration.default.withKebabCaseMemberNames

      import foo._
      val json = json"""{ "this-is-a-field": $thisIsAField, "a": $a, "b": $b}"""

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

  {
    case class FooWithDefault(a: Option[Int] = Some(0), b: String = "b")
    object FooWithDefault {
      implicit val eqConfigExampleFoo: Eq[FooWithDefault] = Eq.fromUniversalEquals
    }

    case class FooNoDefault(a: Option[Int], b: String = "b")
    object FooNoDefault {
      implicit val eqConfigExampleFoo: Eq[FooNoDefault] = Eq.fromUniversalEquals
    }

    implicit val customConfig: Configuration = Configuration.default.withDefaults

    "Option[T] without default" should "be None if null decoded" in {
      val json = json"""{ "a": null }"""
      assert(Decoder[FooNoDefault].decodeJson(json) === Right(FooNoDefault(None, "b")))
    }

    "Option[T] without default" should "be None if missing key decoded" in {
      val json = json"""{}"""
      assert(Decoder[FooNoDefault].decodeJson(json) === Right(FooNoDefault(None, "b")))
    }

    "Option[T] with default" should "be None if null decoded" in {
      val json = json"""{ "a": null }"""
      assert(Decoder[FooWithDefault].decodeJson(json) === Right(FooWithDefault(None, "b")))
    }

    "Option[T] with default" should "be default value if missing key decoded" in {
      val json = json"""{}"""
      assert(Decoder[FooWithDefault].decodeJson(json) === Right(FooWithDefault(Some(0), "b")))
      assert(Decoder[FooWithDefault].decodeAccumulating(json.hcursor) === Validated.valid(FooWithDefault(Some(0), "b")))
    }

    "Value with default" should "be default value if value is null" in {
      val json = json"""{"b": null}"""
      assert(Decoder[FooWithDefault].decodeJson(json) === Right(FooWithDefault(Some(0), "b")))
      assert(Decoder[FooWithDefault].decodeAccumulating(json.hcursor) === Validated.valid(FooWithDefault(Some(0), "b")))
    }

    "Option[T] with default" should "fail to decode if type in json is not correct" in {
      val json = json"""{"a": "NotAnInt"}"""
      assert(Decoder[FooWithDefault].decodeJson(json) === Left(DecodingFailure("Int", List(DownField("a")))))
      assert(
        Decoder[FooWithDefault].decodeAccumulating(json.hcursor)
          === Validated.invalidNel(DecodingFailure("Int", List(DownField("a"))))
      )
    }

    "Field with default" should "fail to decode it type in json is not correct" in {
      val json = json"""{"b": 1}"""
      assert(Decoder[FooWithDefault].decodeJson(json) === Left(DecodingFailure("String", List(DownField("b")))))
      assert(
        Decoder[FooWithDefault].decodeAccumulating(json.hcursor) === Validated.invalidNel(
          DecodingFailure("String", List(DownField("b")))
        )
      )
    }
  }

  "Configuration#discriminator" should "support a field indicating constructor" in {
    forAll { foo: ConfigExampleFoo =>
      implicit val withDefaultsConfig: Configuration = Configuration.default.withDiscriminator("type")

      import foo._
      val json = json"""{ "type": "ConfigExampleFoo", "thisIsAField": $thisIsAField, "a": $a, "b": $b}"""

      assert(Encoder[ConfigExampleBase].apply(foo) === json)
      assert(Decoder[ConfigExampleBase].decodeJson(json) === Right(foo))
    }
  }

  "Configuration#transformConstructorNames" should "support constructor name transformation with snake_case" in forAll {
    foo: ConfigExampleFoo =>
      implicit val snakeCaseConfig: Configuration =
        Configuration.default.withDiscriminator("type").withSnakeCaseConstructorNames

      import foo._
      val json = json"""{ "type": "config_example_foo", "thisIsAField": $thisIsAField, "a": $a, "b": $b}"""

      assert(Encoder[ConfigExampleBase].apply(foo) === json)
      assert(Decoder[ConfigExampleBase].decodeJson(json) === Right(foo))
  }

  "Configuration#transformConstructorNames" should "support constructor name transformation with kebab-case" in forAll {
    foo: ConfigExampleFoo =>
      implicit val kebabCaseConfig: Configuration =
        Configuration.default.withDiscriminator("type").withKebabCaseConstructorNames

      import foo._
      val json = json"""{ "type": "config-example-foo", "thisIsAField": $thisIsAField, "a": $a, "b": $b}"""

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

    "Decoder[Int => Qux[String]]" should "decode partial JSON representations" in forAll {
      (i: Int, s: String, j: Int) =>
        val result = Json
          .obj(
            "a" -> Json.fromString(s),
            "j" -> Json.fromInt(j)
          )
          .as[Int => Qux[String]]
          .map(_(i))

        assert(result === Right(Qux(i, s, j)))
    }
  }
}
