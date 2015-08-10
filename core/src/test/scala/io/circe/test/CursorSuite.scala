package io.circe.test

import algebra.Eq
import cats.data.{ NonEmptyList, Validated, Xor }
import io.circe.{ Cursor, GenericCursor, Json }
import io.circe.syntax._

abstract class CursorSuite[C <: GenericCursor[C]](implicit eq: Eq[C]) extends CirceSuite {
  def fromJson(j: Json): C
  def top(c: C): Option[Json]
  def focus(c: C): Option[Json]
  def fromResult(result: C#Result): Option[C]

  val j1: Json = Json.obj(
    "a" -> (1 to 5).toList.asJson,
    "b" -> Map("d" -> List(true, false, true)).asJson,
    "c" -> Map("e" -> 100.1, "f" -> 200.2).asJson
  )

  val j2: Json = Json.obj(
    "a" -> (0 to 5).toList.asJson,
    "b" -> Map("d" -> List(true, false, true)).asJson,
    "c" -> Map("e" -> 100.1, "f" -> 200.2).asJson
  )

  val j3: Json = Json.obj(
    "a" -> (1 to 5).toList.asJson,
    "b" -> 10.asJson,
    "c" -> Map("e" -> 100.1, "f" -> 200.2).asJson
  )

  val j4: Json = Json.obj(
    "a" -> (1 to 5).toList.asJson,
    "c" -> Map("e" -> 100.1, "f" -> 200.2).asJson
  )

  val cursor: C = fromJson(j1)

  test("withFocus") {
    val result = fromResult(cursor.downField("a")).map(
    	_.withFocus(j =>
    		j.asArray.fold(j)(a => Json.fromValues(0.asJson :: a))
    	)
    )
    
    assert(result.flatMap(top) === Some(j2))
  }

  test("set") {
    val result = fromResult(cursor.downField("b")).map(_.set(10.asJson))

    assert(result.flatMap(top) === Some(j3))
  }

  test("lefts") {
    val result = for {
      c <- fromResult(cursor.downField("a"))
      a <- fromResult(c.downN(3))
      l <- a.lefts
    } yield l

    assert(result === Some(List(3.asJson, 2.asJson, 1.asJson)))
  }

  test("rights") {
    val result = for {
      c <- fromResult(cursor.downField("a"))
      a <- fromResult(c.downN(3))
      l <- a.rights
    } yield l

    assert(result === Some(List(5.asJson)))
  }

  test("fieldSet") {
    assert(fromJson(j1).fieldSet.map(_.toList.sorted) === Some(List("a", "b", "c")))
  }

  test("fields") {
    assert(fromJson(j1).fields === Some(List("a", "b", "c")))
  }

  test("valid left") {
    val result = for {
      c <- fromResult(cursor.downField("a"))
      a <- fromResult(c.downN(3))
      l <- fromResult(a.left)
    } yield l

    assert(result.flatMap(focus) === Some(3.asJson))
  }

  test("invalid left") {
    val result = for {
      c <- fromResult(cursor.downField("b"))
      l <- fromResult(c.left)
    } yield l

    assert(result.flatMap(focus) === None)
  }

  test("valid right") {
    val result = for {
      c <- fromResult(cursor.downField("a"))
      a <- fromResult(c.downN(3))
      l <- fromResult(a.right)
    } yield l

    assert(result.flatMap(focus) === Some(5.asJson))
  }

  test("invalid right") {
    val result = for {
      c <- fromResult(cursor.downField("b"))
      l <- fromResult(c.right)
    } yield l

    assert(result.flatMap(focus) === None)
  }

  test("delete") {
    val result = fromResult(cursor.downField("b")).flatMap(c => fromResult(c.delete))

    assert(result.flatMap(top) === Some(j4))
  }
}
