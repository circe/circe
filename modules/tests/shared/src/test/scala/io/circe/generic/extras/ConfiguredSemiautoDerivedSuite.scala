package io.circe.generic.extras

import cats.Eq
import io.circe.{ Decoder, Encoder, ObjectEncoder }
import io.circe.ast.Json
import io.circe.generic.extras.semiauto._
import io.circe.literal._
import io.circe.tests.CirceSuite
import io.circe.tests.examples._
import shapeless.Witness
import shapeless.labelled.{ field, FieldType }

class ConfiguredSemiautoDerivedSuite extends CirceSuite {
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

  implicit val customConfig: Configuration =
    Configuration.default.withSnakeCaseKeys.withDefaults.withDiscriminator("type")

  implicit val decodeIntlessQux: Decoder[Int => Qux[String]] =
    deriveFor[Int => Qux[String]].incomplete

  implicit val decodeJlessQux: Decoder[FieldType[Witness.`'j`.T, Int] => Qux[String]] =
    deriveFor[FieldType[Witness.`'j`.T, Int] => Qux[String]].incomplete

  implicit val decodeQuxPatch: Decoder[Qux[String] => Qux[String]] = deriveFor[Qux[String]].patch

  implicit val decodeConfigExampleBase: Decoder[ConfigExampleBase] = deriveDecoder
  implicit val encodeConfigExampleBase: ObjectEncoder[ConfigExampleBase] = deriveEncoder

  "Semi-automatic derivation" should "support configuration" in forAll { (f: String, b: Double) =>
    val foo: ConfigExampleBase = ConfigExampleFoo(f, 0, b)
    val json = json"""{ "type": "ConfigExampleFoo", "this_is_a_field": $f, "b": $b}"""
    val expected = json"""{ "type": "ConfigExampleFoo", "this_is_a_field": $f, "a": 0, "b": $b}"""

    assert(Encoder[ConfigExampleBase].apply(foo) === expected)
    assert(Decoder[ConfigExampleBase].decodeJson(json) === Right(foo))
  }

  "Decoder[Int => Qux[String]]" should "decode partial JSON representations" in forAll { (i: Int, s: String, j: Int) =>
    val result = Decoder[Int => Qux[String]].decodeJson(
      Json.obj(
        "a" -> Json.fromString(s),
        "j" -> Json.fromInt(j)
      )
    ).map(_(i))

    assert(result === Right(Qux(i, s, j)))
  }

  "Decoder[FieldType[Witness.`'j`.T, Int] => Qux[String]]" should "decode partial JSON representations" in {
    forAll { (i: Int, s: String, j: Int) =>
      val result = Decoder[FieldType[Witness.`'j`.T, Int] => Qux[String]].decodeJson(
        Json.obj(
          "i" -> Json.fromInt(i),
          "a" -> Json.fromString(s)
        )
      ).map(_(field(j)))

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

      assert(Decoder[Qux[String] => Qux[String]].decodeJson(json).map(_(q)) === Right(expected))
    }
  }
}
