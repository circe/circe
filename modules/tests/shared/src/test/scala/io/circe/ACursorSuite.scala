package io.circe

import cats.data.NonEmptyList
import cats.implicits._
import io.circe.syntax._
import io.circe.tests.CirceMunitSuite
import org.scalacheck.Prop._

class ACursorSuite extends CirceMunitSuite {
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

  val cursor: ACursor = HCursor.fromJson(j1)

  property("focus should return the JSON value in a newly created cursor") {
    forAll { (j: Json) =>
      HCursor.fromJson(j).focus ?= Some(j)
    }
  }

  property("top should return from navigation into an object")(topProp)
  private lazy val topProp = forAll { (j: Json) =>
    val c = HCursor.fromJson(j)

    val intoObject = for {
      keys <- c.keys
      first <- keys.headOption
      atFirst <- c.downField(first).success
    } yield atFirst

    assert(intoObject.forall(atFirst => atFirst.top === Some(j)))
  }

  property("top should return from navigation into an array") {
    forAll { (j: Json) =>
      assert(HCursor.fromJson(j).downArray.success.forall(atFirst => atFirst.top === Some(j)))
    }
  }

  property("root should return from navigation into an object")(rootProp)
  private lazy val rootProp = forAll { (j: Json) =>
    val c = HCursor.fromJson(j)

    val intoObject = for {
      keys <- c.keys
      first <- keys.headOption
      atFirst <- c.downField(first).success
    } yield atFirst

    assert(intoObject.forall(atFirst => atFirst.root.focus === Some(j)))
  }

  property("root should return from navigation into an array") {
    forAll { (j: Json) =>
      HCursor.fromJson(j).downArray.root.focus ?= Some(j)
    }
  }

  property("up should undo navigation into an object")(upUndoObjectProp)
  lazy val upUndoObjectProp = forAll { (j: Json) =>
    val c = HCursor.fromJson(j)

    val intoObject = for {
      keys <- c.keys
      first <- keys.headOption
      atFirst <- c.downField(first).success
    } yield atFirst

    assert(intoObject.forall(_.up.success.flatMap(_.focus) === Some(j)))
  }

  property("up should undo navigation into an array") {
    forAll { (j: Json) =>
      val success = HCursor.fromJson(j).downArray.success
      assert(success.forall(atFirst => atFirst.up.success.flatMap(_.focus) === Some(j)))
    }
  }

  property("up should fail at the top") {
    forAll { (j: Json) =>
      val result = HCursor.fromJson(j).up
      assert(result.failed && result.history === List(CursorOp.MoveUp))
    }
  }

  property("withFocus should have no effect when given the identity function") {
    forAll { (j: Json) =>
      HCursor.fromJson(j).withFocus(identity).focus ?= Some(j)
    }
  }

  test("withFocus should support adding an element to an array") {
    val result = cursor
      .downField("a")
      .success
      .map(
        _.withFocus(j => j.asArray.fold(j)(a => Json.fromValues(0.asJson +: a)))
      )

    assertEquals(result.flatMap(_.top), Some(j2))
  }

  property("withFocusM should lift a value into a List") {
    forAll { (j: Json) =>
      HCursor.fromJson(j).withFocusM[List](List(_)).head.focus ?= Some(j)
    }
  }

  test("delete should remove a value from an object") {
    val result = cursor.downField("b").success.flatMap(_.delete.success)

    assertEquals(result.flatMap(_.top), Some(j4))
  }

  property("delete should remove a value from an array")(deleteArrayProp)
  private lazy val deleteArrayProp = forAll { (h: Json, t: List[Json]) =>
    val result = for {
      f <- HCursor.fromJson(Json.fromValues(h :: t)).downArray.success
      u <- f.delete.success
    } yield u

    result.flatMap(_.focus) ?= Some(Json.fromValues(t))
  }

  property("delete should fail at the top") {
    forAll { (j: Json) =>
      val result = HCursor.fromJson(j).delete

      assert(result.failed && result.history === List(CursorOp.DeleteGoParent))
    }
  }

  test("set should replace an element") {
    val result = cursor.downField("b").success.map(_.set(10.asJson))

    assertEquals(result.flatMap(_.top), Some(j3))
  }

  test("values should return the expected values") {
    assertEquals(cursor.downField("a").values.map(_.toVector), Some((1 to 5).toVector.map(_.asJson)))
  }

  test("keys should return the expected values") {
    assertEquals(cursor.keys.map(_.toVector), Some(Vector("a", "b", "c")))
  }

  test("left should successfully select an existing value") {
    val result = for {
      c <- cursor.downField("a").success
      a <- c.downN(3).success
      l <- a.left.success
    } yield l

    assertEquals(result.flatMap(_.focus), Some(3.asJson))
  }

  test("left should fail to select a value that doesn't exist") {
    val result = for {
      c <- cursor.downField("b").success
      l <- c.left.success
    } yield l

    assertEquals(result.flatMap(_.focus), None)
  }

  property("left should fail at the top") {
    forAll { (j: Json) =>
      val result = HCursor.fromJson(j).left
      assert(result.failed && result.history === List(CursorOp.MoveLeft))
    }
  }

  test("right should successfully select an existing value") {
    val result = for {
      c <- cursor.downField("a").success
      a <- c.downN(3).success
      r <- a.right.success
    } yield r

    assertEquals(result.flatMap(_.focus), Some(5.asJson))
  }

  test("right should fail to select a value that doesn't exist") {
    val result = for {
      c <- cursor.downField("b").success
      r <- c.right.success
    } yield r

    assertEquals(result.flatMap(_.focus), None)
  }

  property("right should fail at the top") {
    forAll { (j: Json) =>
      val result = HCursor.fromJson(j).right
      assert(result.failed && result.history === List(CursorOp.MoveRight))
    }
  }

  test("downArray should successfully select an existing value") {
    val result = for {
      c <- cursor.downField("a").success
      a <- c.downN(3).success
      f <- a.up.downArray.success
    } yield f

    assertEquals(result.flatMap(_.focus), Some(1.asJson))
  }

  test("downArray should fail to select a value that doesn't exist") {
    val result = for {
      c <- cursor.downField("b").success
      f <- c.up.downArray.success
    } yield f

    assertEquals(result.flatMap(_.focus), None)
  }

  test("field should successfully select an existing value") {
    val result = for {
      c <- cursor.downField("c").success
      e <- c.downField("e").success
      f <- e.field("f").success
    } yield f

    assertEquals(result.flatMap(_.focus), Some(200.2.asJson))
  }

  property("field should fail at the top") {
    forAll { (j: Json, key: String) =>
      val result = HCursor.fromJson(j).field(key)
      assert(result.failed && result.history === List(CursorOp.Field(key)))
    }
  }

  test("getOrElse should successfully decode an existing field") {
    val result = for {
      b <- cursor.downField("b").success
    } yield b.getOrElse[List[Boolean]]("d")(Nil)
    assertEquals(result, Some(Right(List(true, false, true))))
  }

  test("getOrElse should use the fallback if field is missing") {
    val result = for {
      b <- cursor.downField("b").success
    } yield b.getOrElse[List[Boolean]]("z")(Nil)
    assertEquals(result, Some(Right(Nil)))
  }

  test("getOrElse should fail if the field is the wrong type") {
    val result = for {
      b <- cursor.downField("b").success
    } yield b.getOrElse[List[Int]]("d")(Nil)
    assert(result.fold(false)(_.isLeft))
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
        "a" -> Json.arr(Json.fromString("string"), Json.obj("b" -> Json.fromInt(1), "c" -> Json.fromInt(2))),
        "c" -> Json.fromBoolean(true),
        "d" -> JsonObject.fromMap(Map("e" -> Json.JNull)).asJson,
        "e" -> JsonObject.fromMap(Map("f" -> Json.arr(Json.JNull))).asJson,
        "f" -> Json.arr(
          JsonObject.fromMap(Map("g" -> JsonObject.fromMap(Map("h" -> Json.arr(Json.Null))).asJson)).asJson
        ),
        "g" -> Json.arr(
          JsonObject.fromMap(Map("g" -> JsonObject.fromMap(Map("h" -> Json.arr())).asJson)).asJson
        )
      )
    val c: ACursor = HCursor.fromJson(json)

    assertEquals(
      c.pathString,
      ""
    )

    assertEquals(
      c.downField("a").pathString,
      ".a"
    )

    assertEquals(
      c.downField("a").downArray.pathString,
      ".a[0]"
    )

    assertEquals(
      c.downField("a").downN(1).downField("b").pathString,
      ".a[1].b"
    )

    assertEquals(
      c.downField("a").downN(1).downField("b").up.left.right.left.pathString,
      ".a[0]"
    )

    assertEquals(
      c.downField("a").downN(1).downField("b").field("c").up.left.right.left.pathString,
      ".a[0]"
    )

    assertEquals(
      c.downField("d").downField("e").pathString,
      ".d.e"
    )

    assertEquals(
      c.downField("e").downField("f").downArray.pathString,
      ".e.f[0]"
    )

    assertEquals(
      c.downField("f").downArray.downField("g").downField("h").downArray.pathString,
      ".f[0].g.h[0]"
    )

    assertEquals(
      c.downField("g").downArray.downField("g").downField("h").downArray.pathString,
      ".g[0].g.h[0]"
    )

    assertEquals(
      c.downField("g").field("f").downArray.pathString,
      ".f[0]"
    )

    assertEquals(
      Decoder[NonEmptyList[String]].decodeJson(Json.arr(Json.fromString(""), Json.fromInt(1))).leftMap(_.show),
      Left(
        "DecodingFailure at [1]: Got value '1' with wrong type, expecting string"
      )
    )

    assertEquals(
      Decoder[NonEmptyList[String]].decodeJson(Json.arr(Json.fromString(""), Json.fromInt(1))).leftMap(_.getMessage),
      Left(
        "Got value '1' with wrong type, expecting string: DownArray,DeleteGoParent,DownArray"
      )
    )
  }

  test("benchmark") {
    val dataLotsOfArrays = (0 to 100).foldLeft(Json.True) { (acc, _) =>
      Json.fromValues {
        (0 to 99).map(_.asJson) :+ acc
      }
    }
    val cursorLotsOfArrays = (0 to 100).foldLeft[ACursor](dataLotsOfArrays.hcursor) { (acc, _) =>
      (0 to 99).foldLeft(acc.downArray) { (acc, _) =>
        acc.right
      }
    }
    cursorLotsOfArrays.pathString
  }
}
