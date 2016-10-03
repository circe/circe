package io.circe.optics

import io.circe._
import io.circe.generic.semiauto._
import io.circe.optics.semiauto._
import io.circe.syntax._
import io.circe.tests.CirceSuite
import monocle.Iso

class SemiautoSuite extends CirceSuite {

  case class UserSnake(id: Long, first_name: String, last_name: String)
  case class UserCamel(id: Long, firstName: String, lastName: String)
  
  val snake2camel = Iso[UserSnake, UserCamel] {
    (s: UserSnake) => UserCamel(s.id, s.first_name, s.last_name)
  } {
    (s: UserCamel) => UserSnake(s.id, s.firstName, s.lastName)
  }

  val john: Json = Json.obj(
    "id"         -> 1.asJson,
    "first_name" -> "John".asJson,
    "last_name"  -> "Doe".asJson
  )

  "deriveDecoderWithIso[A, B]" should "create a decoder[B] with an Iso[A, B]" in {
    implicit val iso: Iso[UserSnake, UserCamel] = snake2camel
    implicit val snakeDecoder: Decoder[UserSnake] = deriveDecoder[UserSnake]

    val camelDecoder: Decoder[UserCamel] = deriveDecoderWithIso[UserSnake, UserCamel]

    assert(camelDecoder.decodeJson(john) == Right(UserCamel(1, "John", "Doe")))
  }

  "deriveEncoderWithIso[A, B]" should "create an encoder[B] with an Iso[A, B]" in {
    implicit val iso: Iso[UserSnake, UserCamel] = snake2camel
    implicit val snakeEncoder: Encoder[UserSnake] = deriveEncoder[UserSnake]

    val camelEncoder: Encoder[UserCamel] = deriveEncoderWithIso[UserSnake, UserCamel]

    assert(camelEncoder(UserCamel(1, "John", "Doe")) == john)
  }

  "deriveDecoderWithIso[B, A]" should "create a decoder[B] with an Iso[B, A]" in {
    implicit val iso: Iso[UserCamel, UserSnake] = snake2camel.reverse
    implicit val snakeDecoder: Decoder[UserSnake] = deriveDecoder[UserSnake]

    val camelDecoder: Decoder[UserCamel] = deriveDecoderWithIso[UserCamel, UserSnake]

    assert(camelDecoder.decodeJson(john) == Right(UserCamel(1, "John", "Doe")))
  }

  "deriveEncoderWithIso[B, A]" should "create an encoder[B] with an Iso[B, A]" in {
    implicit val iso: Iso[UserCamel, UserSnake] = snake2camel.reverse
    implicit val snakeEncoder: Encoder[UserSnake] = deriveEncoder[UserSnake]

    val camelEncoder: Encoder[UserCamel] = deriveEncoderWithIso[UserCamel, UserSnake]

    assert(camelEncoder(UserCamel(1, "John", "Doe")) == john)
  }
}
