package io.circe

import cats._
import cats.syntax.all._
import io.circe.syntax._
import io.circe.tests.CirceMunitSuite
import org.scalacheck.Prop
import org.scalacheck.Prop._

final class CursorSuite extends CirceMunitSuite {
  import CursorSuite._

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

  val cursor: Cursor = Cursor.fromJson(j1)

  property("focus should return the JSON value in a newly created cursor") {
    forAll { (j: Json) =>
      Cursor.fromJson(j).focus ?= Some(j)
    }
  }

  property("Navigation into an object")(topProp)
  private lazy val topProp = forAll { (j: Json) =>
    val c = Cursor.fromJson(j)

    val intoObject = for {
      keys <- c.keys
      first <- keys.headOption
    } yield c.downField(first)

    forAllP(intoObject)(value =>
      ((value.top.fold(Prop.passed)(_ ?= j)) :| "Should return to top") &&
        ((value.root.focus.fold(Prop.passed)(_ ?= j)) :| "Should return to root") &&
        ((value.up.focus.fold(Prop.passed)(_ ?= j)) :| "Up should undo this operation")
    )
  }

  property("Navigation into an array") {
    forAll { (j: Json) =>
      val c: Cursor = Cursor.fromJson(j).downArray

      ((c.top.fold(Prop.passed)(_ ?= j)) :| "Should return to top") &&
      ((c.root.focus.fold(Prop.passed)(_ ?= j)) :| "Should return to root") &&
      ((c.up.focus.fold(Prop.passed)(_ ?= j)) :| "Up should undo this operation")
    }
  }

  property("Up should fail at the top") {
    forAll { (j: Json) =>
      val result = Cursor.fromJson(j).up
      (Prop(result.failed) :| "Is failed") && ((result.history.asList ?= List(
        CursorOp.MoveUp
      )) :| "Have correct history")
    }
  }

  property("mapFocus should have no effect when given the identity function") {
    forAll { (j: Json) =>
      Cursor.fromJson(j).mapFocus(identity).focus ?= Some(j)
    }
  }

  test("mapFocus should support adding an element to an array") {
    val result: Cursor = cursor.downField("a").mapFocus(j => j.asArray.fold(j)(a => Json.fromValues(0.asJson +: a)))

    assertEquals(result.top, Some(j2))
  }

  property("mapFocusA should lift a value into a List") {
    forAll { (j: Json) =>
      Cursor.fromJson(j).mapFocusA[List](List(_)).head.focus ?= Some(j)
    }
  }

  test("delete should remove a value from an object") {
    val result: Cursor = cursor.downField("b").delete

    assertEquals(result.top, Some(j4))
  }

  property("delete should remove a value from an array")(deleteArrayProp)
  private lazy val deleteArrayProp = forAll { (h: Json, t: List[Json]) =>
    val result: Cursor = Cursor.fromJson(Json.fromValues(h :: t)).downArray.delete

    result.focus ?= Some(Json.fromValues(t))
  }

  property("delete should fail at the top") {
    forAll { (j: Json) =>
      val result: Cursor = Cursor.fromJson(j).delete

      (Prop(result.failed) :| "Is failed") && ((result.history.asList ?= List(
        CursorOp.DeleteGoParent
      )) :| "Has correct history")
    }
  }

  test("set should replace an element") {
    val result: Cursor = cursor.downField("b").set(10.asJson)

    assertEquals(result.top, Some(j3))
  }

  test("values should return the expected values") {
    assertEquals(cursor.downField("a").values.map(_.toVector), Some((1 to 5).toVector.map(_.asJson)))
  }

  test("keys should return the expected values") {
    assertEquals(cursor.keys.map(_.toVector), Some(Vector("a", "b", "c")))
  }

  test("left should successfully select an existing value") {
    val result: Cursor = cursor.downField("a").downN(3).left

    assertEquals(result.focus, Some(3.asJson))
  }

  test("left should fail to select a value that doesn't exist") {
    val result: Cursor = cursor.downField("b").left

    assertEquals(result.focus, None)
  }

  property("left should fail at the top") {
    forAll { (j: Json) =>
      val result: Cursor = Cursor.fromJson(j).left
      (Prop(result.failed) :| "Is Failed") && ((result.history.asList ?= List(
        CursorOp.MoveLeft
      )) :| "Has correct history")
    }
  }

  test("right should successfully select an existing value") {
    val result: Cursor = cursor.downField("a").downN(3).right

    assertEquals(result.focus, Some(5.asJson))
  }

  test("right should fail to select a value that doesn't exist") {
    val result: Cursor = cursor.downField("b").right

    assertEquals(result.focus, None)
  }

  property("right should fail at the top") {
    forAll { (j: Json) =>
      val result: Cursor = Cursor.fromJson(j).right

      (Prop(result.failed) :| "Is failed") && ((result.history.asList ?= List(
        CursorOp.MoveRight
      )) :| "Has correct history")
    }
  }

  test("downArray should successfully select an existing value") {
    val result: Cursor = cursor.downField("a").downN(3).up.downArray

    assertEquals(result.focus, Some(1.asJson))
  }

  test("downArray should fail to select a value that doesn't exist") {
    val result: Cursor = cursor.downField("b").up.downArray

    assertEquals(result.focus, None)
  }

  test("field should successfully select an existing value") {
    val result: Cursor = cursor.downField("c").downField("e").field("f")

    assertEquals(result.focus, Some(200.2.asJson))
  }

  property("field should fail at the top") {
    forAll { (j: Json, key: String) =>
      val result: Cursor = Cursor.fromJson(j).field(key)
      (Prop(result.failed) :| "Is failed") && ((result.history.asList ?= List(
        CursorOp.Field(key)
      )) :| "Has correct history")
    }
  }

  test("getOrElse should successfully decode an existing field") {
    val result: Decoder.Result[List[Boolean]] = cursor.downField("b").getOrElse[List[Boolean]]("d")(Nil)

    assertEquals(result, Right(List(true, false, true)))
  }

  test("getOrElse should use the fallback if field is missing") {
    val result: Decoder.Result[List[Boolean]] = cursor.downField("b").getOrElse[List[Boolean]]("z")(Nil)

    assertEquals(result, Right(Nil))
  }

  test("getOrElse should fail if the field is the wrong type") {
    val result: Decoder.Result[List[Int]] = cursor.downField("b").getOrElse[List[Int]]("d")(Nil)

    assert(result.isLeft)
  }

  test("key should return the key if the cursor is in an object") {
    assertEquals(cursor.downField("b").downField("d").key, Some("d"))
  }

  test("key should return None if the cursor is not in an object") {
    assertEquals(cursor.downField("a").downN(1).key, None)
  }

  test("key should return None if the cursor has failed") {
    assertEquals(cursor.downField("a").downField("XYZ").key, None)
  }

  test("key should return None if the cursor has at the root") {
    assertEquals(cursor.key, None)
  }

  test("index should return the index if the cursor is in an array") {
    assertEquals(cursor.downField("a").downN(1).index, Some(1))
  }

  test("index should return None if the cursor is not in an array") {
    assertEquals(cursor.downField("b").downField("d").index, None)
  }

  test("index should return None if the cursor has failed") {
    assertEquals(cursor.downField("a").downN(10).index, None)
  }

  test("index should return None if the cursor has at the root") {
    assertEquals(cursor.index, None)
  }

  test("pathString should return the correct paths") {
    val json: Json =
      Json.obj(
        "a" -> Json.arr(Json.fromString("string"), Json.obj("b" -> Json.fromInt(1))),
        "c" -> Json.fromBoolean(true)
      )
    val c: Cursor = Cursor.fromJson(json)

    assertEquals(
      c.cursorPathString,
      ""
    )

    assertEquals(
      c.downField("a").cursorPathString,
      ".a"
    )

    assertEquals(
      c.downField("a").downArray.cursorPathString,
      ".a[0]"
    )

    assertEquals(
      c.downField("a").downN(1).downField("b").cursorPathString,
      ".a[1].b"
    )

    assertEquals(
      c.downField("a").downN(1).downField("b").up.left.right.left.cursorPathString,
      ".a[0]"
    )
  }
}

object CursorSuite {

  // Need to add to Scalacheck
  def all[F[_]: Foldable](fa: F[Prop]): Prop =
    fa.foldLeft(Prop.passed) {
      case (acc, value) =>
        acc && value
    }

  // Need to add to Scalacheck
  def forAllP[F[_]: Foldable: Functor, A](fa: F[A])(f: A => Prop): Prop =
    all(fa.map(f))
}
