package io.circe.generic.extras

import cats.kernel.Eq
import io.circe.{ Decoder, Encoder, Json, ObjectEncoder }
import io.circe.generic.extras.semiauto._
import io.circe.literal._
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import io.circe.tests.examples._
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Arbitrary.arbitrary
import shapeless.Witness
import shapeless.labelled.{ field, FieldType }

object ConfiguredSemiautoDerivedSuite {
  object localExamples {
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
  }

  import localExamples._

  implicit val customConfig: Configuration =
    Configuration.default.withSnakeCaseMemberNames.withDefaults.withDiscriminator("type").withSnakeCaseConstructorNames

  implicit val decodeIntlessQux: Decoder[Int => Qux[String]] =
    deriveFor[Int => Qux[String]].incomplete

  implicit val decodeJlessQux: Decoder[FieldType[Witness.`'j`.T, Int] => Qux[String]] =
    deriveFor[FieldType[Witness.`'j`.T, Int] => Qux[String]].incomplete

  implicit val decodeQuxPatch: Decoder[Qux[String] => Qux[String]] = deriveFor[Qux[String]].patch

  implicit val decodeConfigExampleBase: Decoder[ConfigExampleBase] = deriveDecoder
  implicit val encodeConfigExampleBase: ObjectEncoder[ConfigExampleBase] = deriveEncoder
}

class ConfiguredSemiautoDerivedSuite extends CirceSuite {
  import ConfiguredSemiautoDerivedSuite._, localExamples._

  checkLaws("Codec[ConfigExampleBase]", CodecTests[ConfigExampleBase].codec)

  "Semi-automatic derivation" should "support configuration" in forAll { (f: String, b: Double) =>
    val foo: ConfigExampleBase = ConfigExampleFoo(f, 0, b)
    val json = json"""{ "type": "config_example_foo", "this_is_a_field": $f, "b": $b}"""
    val expected = json"""{ "type": "config_example_foo", "this_is_a_field": $f, "a": 0, "b": $b}"""

    assert(Encoder[ConfigExampleBase].apply(foo) === expected)
    assert(Decoder[ConfigExampleBase].decodeJson(json) === Right(foo))
  }

  "Semi-automatice derivation" should "call field modification times equal to field count" in {
    var transformMemberNamesCallCount, transformConstructorCallCount = 0
    implicit val customConfig: Configuration =
      Configuration.default.copy(transformMemberNames = v => {
        transformMemberNamesCallCount = transformMemberNamesCallCount + 1
        Configuration.snakeCaseTransformation(v)
      }, transformConstructorNames = v => {
        transformConstructorCallCount = transformConstructorCallCount + 1
        Configuration.snakeCaseTransformation(v)
      })

    val fieldCount = 3
    val decodeConstructorCount = 2
    val encodeConstructorCount = 1

    val encoder: Encoder[ConfigExampleBase] = deriveEncoder
    val decoder: Decoder[ConfigExampleBase] = deriveDecoder
    for {
      _ <- 1 until 100
    } {
      val foo: ConfigExampleBase = ConfigExampleFoo("field_value", 0, 100)
      val encoded = encoder.apply(foo)
      val decoded = decoder.decodeJson(encoded)
      assert(decoded === Right(foo))
      assert(transformMemberNamesCallCount === fieldCount * 2)
      assert(transformConstructorCallCount === decodeConstructorCount + encodeConstructorCount)
    }
  }

  "Decoder[Int => Qux[String]]" should "decode partial JSON representations" in forAll { (i: Int, s: String, j: Int) =>
    val result = Json.obj(
      "a" -> Json.fromString(s),
      "j" -> Json.fromInt(j)
    ).as[Int => Qux[String]].map(_(i))

    assert(result === Right(Qux(i, s, j)))
  }

  "Decoder[FieldType[Witness.`'j`.T, Int] => Qux[String]]" should "decode partial JSON representations" in {
    forAll { (i: Int, s: String, j: Int) =>
      val result = Json.obj(
        "i" -> Json.fromInt(i),
        "a" -> Json.fromString(s)
      ).as[FieldType[Witness.`'j`.T, Int] => Qux[String]].map(
         _(field(j))
      )

      assert(result === Right(Qux(i, s, j)))
    }
  }

  "Decoder[Qux[String] => Qux[String]]" should "decode patch JSON representations" in {
    forAll { (q: Qux[String], i: Option[Int], a: Option[String], j: Option[Int]) =>
      val json = Json.obj(
        "i" -> Encoder[Option[Int]].apply(i),
        "a" -> Encoder[Option[String]].apply(a),
        "j" -> Encoder[Option[Int]].apply(j)
      )

      val expected = Qux[String](i.getOrElse(q.i), a.getOrElse(q.a), j.getOrElse(q.j))

      assert(json.as[Qux[String] => Qux[String]].map(_(q)) === Right(expected))
    }
  }
}
