package io.circe.optics

import cats.Eq
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._
import io.circe.tests.CirceSuite
import io.circe.optics.JsonPath.root

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
    assert(root.address.street_number.int.getOption(john) === Some(12))
  }

  it should "support traversal by array index" in {
    assert(root.cars.index(1).model.string.getOption(john) === Some("suv"))
  }

  it should "support traversal by array index using apply" in {
    assert(root.cars(1).model.string.getOption(john) === Some("suv"))
  }

  it should "support traversal by array index using apply on the root" in {
    val jsonArray = List("first".asJson, "second".asJson).asJson
    assert(root(0).string.getOption(jsonArray) === Some("first"))
  }

  it should "support insertion and deletion" in {
    assert(root.at("first_name").setOption(None)(john) === john.asObject.map(_.remove("first_name").asJson))
    assert(root.at("foo").set(Some(true.asJson))(john).asObject.flatMap(_.apply("foo")) === Some(Json.True))
  }

  it should "support codec" in {
    assert(root.cars.index(0).as[Car].getOption(john) === Some(Car("fancy", 120, automatic = false)))
  }

  "JsonTraversalPath" should "support traversal over each values of a json object" in {
    assert(root.each.string.getAll(john) === List("John", "Doe"))
  }

  it should "support traversal over each values of a json array" in {
    assert(root.cars.each.maxSpeed.int.getAll(john) === List(120, 80))
  }

  it should "support filtering by field of json object" in {
    assert(root.filterByField(_.contains("first")).string.getAll(john) === List("John"))
  }

  it should "support filtering by index of json array" in {
    assert(root.cars.filterByIndex(_ % 2 == 1).as[Car].getAll(john) === List(Car("suv", 80, automatic = true)))
  }

  it should "support a safe filtering by value" in {
    assert(
      root.cars.each.filter(root.maxSpeed.int.exist(_ > 100)).model.string.getAll(john) === List("fancy")
    )
  }

  it should "support an unsafe filtering by value" in {
    assert(
      root.cars.each.filterUnsafe(root.maxSpeed.int.exist(_ > 100)).model.string.set("new")(john) ===
        Json.obj(
          "first_name" -> "John".asJson,
          "last_name"  -> "Doe".asJson,
          "age"        -> 25.asJson,
          "address"    -> Json.obj(
            "street_number" -> 12.asJson,
            "street_name"   -> "High Street".asJson
          ),
          "cars" -> List(
            Car("new", 120, automatic = false),
            Car("suv", 80, automatic = true)
          ).asJson
        )
    )
  }

}
