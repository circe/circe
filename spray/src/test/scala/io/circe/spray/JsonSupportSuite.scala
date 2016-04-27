package io.circe.spray

import io.circe.{ Decoder, Encoder, ObjectEncoder }
import org.scalatest.FlatSpec
import spray.httpx.unmarshalling._
import spray.httpx.marshalling._
import spray.http.{ ContentTypes, HttpCharsets, HttpEntity, MediaTypes }

/**
 * Copied from spray-httpx's test suite.
 */
case class Employee(fname: String, name: String, age: Int, id: Long, boardMember: Boolean) {
  require(!boardMember || age > 40, "Board members must be older than 40")
}

object Employee {
  val simple: Employee = Employee("Frank", "Smith", 42, 12345, false)
  val json: String = """{"fname":"Frank","name":"Smith","age":42,"id":12345,"boardMember":false}"""

  val utf8: Employee = Employee("Fränk", "Smi√", 42, 12345, false)
  val utf8json: Array[Byte] =
    """{
      |  "fname": "Fränk",
      |  "name": "Smi√",
      |  "age": 42,
      |  "id": 12345,
      |  "boardMember": false
      |}""".stripMargin.getBytes(HttpCharsets.`UTF-8`.nioCharset)

  val illegalEmployeeJson: String =
    """{"fname":"Little Boy","name":"Smith","age":7,"id":12345,"boardMember":true}"""

  implicit val decodeEmployee: Decoder[Employee] =
    Decoder.forProduct5("fname", "name", "age", "id", "boardMember")(Employee.apply)

  implicit val encodeEmployee: ObjectEncoder[Employee] =
    Encoder.forProduct5("fname", "name", "age", "id", "boardMember") {
      case Employee(fname, name, age, id, boardMember) => (fname, name, age, id, boardMember)
    }
}

class JsonSupportSpec extends FlatSpec {
  import JsonSupport._

  "JsonSupport" should "provide unmarshalling support for a case class" in {
    val expected = Right(Employee.simple)

    assert(HttpEntity(ContentTypes.`application/json`, Employee.json).as[Employee] === expected)
  }

  it should "provide marshalling support for a case class" in {
    val expected = Right(HttpEntity(ContentTypes.`application/json`, Employee.json))

    assert(marshal(Employee.simple) === expected)
  }

  it should "use UTF-8 as the default charset for JSON source decoding" in {
    val expected = Right(Employee.utf8)

    assert(HttpEntity(MediaTypes.`application/json`, Employee.utf8json).as[Employee] === expected)
  }

  it should "provide proper error messages for requirement errors" in {
    val Left(MalformedContent(msg, Some(ex: IllegalArgumentException))) =
      HttpEntity(MediaTypes.`application/json`, Employee.illegalEmployeeJson).as[Employee]

    assert(msg === "requirement failed: Board members must be older than 40")
  }
}
