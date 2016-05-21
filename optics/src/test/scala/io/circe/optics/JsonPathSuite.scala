package io.circe.optics

import cats.Eq
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import io.circe.tests.CirceSuite

class JsonPathSuite extends CirceSuite {

  case class Car(model: String, maxSpeed: Int, automatic: Boolean)
  object  Car {
    implicit val eq: Eq[Car] = Eq.fromUniversalEquals[Car]
    implicit val decoder: Decoder[Car] = deriveDecoder[Car]
    implicit val encoder: ObjectEncoder[Car] = deriveEncoder[Car]
  }

  val john: Json = Json.obj(
    "first_name" -> "John".asJson,
    "last_name"  -> "Doe".asJson,
    "age"        -> 25.asJson,
    "address"    -> Json.obj(
      "street_number" -> 12.asJson,
      "street_name"   -> "High Street".asJson
    ),
    "cars" -> List(
      Car("fancy", 120, automatic = false),
      Car("suv", 80, automatic = true)
    ).asJson
  )

  "JsonPath" should "support traversal by field name" in {
    assert(JsonPath.root.address.street_number.int.getOption(john) === Some(12))
  }

  it should "support traversal by array index" in {
    assert(JsonPath.root.cars.index(1).model.string.getOption(john) === Some("suv"))
  }

  it should "support insertion and deletion" in {
    assert(JsonPath.root.at("first_name").setOption(None)(john) === john.asObject.map(_.remove("first_name").asJson))
    assert(JsonPath.root.at("foo").set(Some(true.asJson))(john).asObject.flatMap(_.apply("foo")) === Some(Json.True))
  }

  it should "support codec" in {
    assert(JsonPath.root.cars.index(0).as[Car].getOption(john) === Some(Car("fancy", 120, automatic = false)))
  }

  "JsonTraversalPath" should "support traversal over each values of a json object" in {
    assert(JsonPath.root.each.string.getAll(john) === List("John", "Doe"))
  }

  it should "support traversal over each values of a json array" in {
    assert(JsonPath.root.cars.each.maxSpeed.int.getAll(john) === List(120, 80))
  }

  it should "support filtering of json object" in {
    assert(JsonPath.root.objFilter(_.contains("first")).string.getAll(john) === List("John"))
  }

  it should "support filtering of json array" in {
    assert(JsonPath.root.cars.arrayFilter(_ % 2 == 1).as[Car].getAll(john) === List(Car("suv", 80, automatic = true)))
  }

}
