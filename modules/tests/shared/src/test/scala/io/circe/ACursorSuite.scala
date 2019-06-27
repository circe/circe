package io.circe

import io.circe.syntax._
import io.circe.tests.CirceSuite

class ACursorSuite extends CirceSuite {
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

  "focus" should "return the JSON value in a newly created cursor" in forAll { (j: Json) =>
    assert(HCursor.fromJson(j).focus === Some(j))
  }

  "top" should "return from navigation into an object" in forAll { (j: Json) =>
    val c = HCursor.fromJson(j)

    val intoObject = for {
      keys <- c.keys
      first <- keys.headOption
      atFirst <- c.downField(first).success
    } yield atFirst

    assert(intoObject.forall(atFirst => atFirst.top === Some(j)))
  }

  it should "return from navigation into an array" in forAll { (j: Json) =>
    assert(HCursor.fromJson(j).downArray.success.forall(atFirst => atFirst.top === Some(j)))
  }

  "up" should "undo navigation into an object" in forAll { (j: Json) =>
    val c = HCursor.fromJson(j)

    val intoObject = for {
      keys <- c.keys
      first <- keys.headOption
      atFirst <- c.downField(first).success
    } yield atFirst

    assert(intoObject.forall(_.up.success.flatMap(_.focus) === Some(j)))
  }

  it should "undo navigation into an array" in forAll { (j: Json) =>
    assert(
      HCursor.fromJson(j).downArray.success.forall(atFirst => atFirst.up.success.flatMap(_.focus) === Some(j))
    )
  }

  it should "fail at the top" in forAll { (j: Json) =>
    val result = HCursor.fromJson(j).up

    assert(result.failed && result.history === List(CursorOp.MoveUp))
  }

  "withFocus" should "have no effect when given the identity function" in forAll { (j: Json) =>
    assert(HCursor.fromJson(j).withFocus(identity).focus === Some(j))
  }

  it should "support adding an element to an array" in {
    val result = cursor
      .downField("a")
      .success
      .map(
        _.withFocus(j => j.asArray.fold(j)(a => Json.fromValues(0.asJson +: a)))
      )

    assert(result.flatMap(_.top) === Some(j2))
  }

  "withFocusM" should "lift a value into a List" in forAll { (j: Json) =>
    assert(HCursor.fromJson(j).withFocusM[List](List(_)).head.focus === Some(j))
  }

  "delete" should "remove a value from an object" in {
    val result = cursor.downField("b").success.flatMap(_.delete.success)

    assert(result.flatMap(_.top) === Some(j4))
  }

  it should "remove a value from an array" in forAll { (h: Json, t: List[Json]) =>
    val result = for {
      f <- HCursor.fromJson(Json.fromValues(h :: t)).downArray.success
      u <- f.delete.success
    } yield u

    assert(result.flatMap(_.focus) === Some(Json.fromValues(t)))
  }

  it should "fail at the top" in forAll { (j: Json) =>
    val result = HCursor.fromJson(j).delete

    assert(result.failed && result.history === List(CursorOp.DeleteGoParent))
  }

  "set" should "replace an element" in {
    val result = cursor.downField("b").success.map(_.set(10.asJson))

    assert(result.flatMap(_.top) === Some(j3))
  }

  "lefts" should "return the expected values" in {
    val result = for {
      c <- cursor.downField("a").success
      a <- c.downN(3).success
      l <- a.lefts
    } yield l

    assert(result === Some(Vector(3.asJson, 2.asJson, 1.asJson)))
  }

  it should "fail at the top" in forAll { (j: Json) =>
    assert(HCursor.fromJson(j).lefts === None)
  }

  "rights" should "return the expected values" in {
    val result = for {
      c <- cursor.downField("a").success
      a <- c.downN(3).success
      l <- a.rights
    } yield l

    assert(result === Some(Vector(5.asJson)))
  }

  it should "fail at the top" in forAll { (j: Json) =>
    assert(HCursor.fromJson(j).rights === None)
  }

  "values" should "return the expected values" in {
    assert(cursor.downField("a").values.map(_.toVector) === Some((1 to 5).toVector.map(_.asJson)))
  }

  "keys" should "return the expected values" in {
    assert(cursor.keys.map(_.toVector) === Some(Vector("a", "b", "c")))
  }

  "left" should "successfully select an existing value" in {
    val result = for {
      c <- cursor.downField("a").success
      a <- c.downN(3).success
      l <- a.left.success
    } yield l

    assert(result.flatMap(_.focus) === Some(3.asJson))
  }

  it should "fail to select a value that doesn't exist" in {
    val result = for {
      c <- cursor.downField("b").success
      l <- c.left.success
    } yield l

    assert(result.flatMap(_.focus) === None)
  }

  it should "fail at the top" in forAll { (j: Json) =>
    val result = HCursor.fromJson(j).left

    assert(result.failed && result.history === List(CursorOp.MoveLeft))
  }

  "right" should "successfully select an existing value" in {
    val result = for {
      c <- cursor.downField("a").success
      a <- c.downN(3).success
      r <- a.right.success
    } yield r

    assert(result.flatMap(_.focus) === Some(5.asJson))
  }

  it should "fail to select a value that doesn't exist" in {
    val result = for {
      c <- cursor.downField("b").success
      r <- c.right.success
    } yield r

    assert(result.flatMap(_.focus) === None)
  }

  it should "fail at the top" in forAll { (j: Json) =>
    val result = HCursor.fromJson(j).right

    assert(result.failed && result.history === List(CursorOp.MoveRight))
  }

  "first" should "successfully select an existing value" in {
    val result = for {
      c <- cursor.downField("a").success
      a <- c.downN(3).success
      f <- a.first.success
    } yield f

    assert(result.flatMap(_.focus) === Some(1.asJson))
  }

  it should "fail to select a value that doesn't exist" in {
    val result = for {
      c <- cursor.downField("b").success
      f <- c.first.success
    } yield f

    assert(result.flatMap(_.focus) === None)
  }

  it should "fail at the top" in forAll { (j: Json) =>
    val result = HCursor.fromJson(j).first

    assert(result.failed && result.history === List(CursorOp.MoveFirst))
  }

  "last" should "successfully select an existing value" in {
    val result = for {
      c <- cursor.downField("a").success
      a <- c.downN(3).success
      l <- a.last.success
    } yield l

    assert(result.flatMap(_.focus) === Some(5.asJson))
  }

  it should "fail to select a value that doesn't exist" in {
    val result = for {
      c <- cursor.downField("b").success
      l <- c.last.success
    } yield l

    assert(result.flatMap(_.focus) === None)
  }

  it should "fail at the top" in forAll { (j: Json) =>
    val result = HCursor.fromJson(j).last

    assert(result.failed && result.history === List(CursorOp.MoveLast))
  }

  "field" should "successfully select an existing value" in {
    val result = for {
      c <- cursor.downField("c").success
      e <- c.downField("e").success
      f <- e.field("f").success
    } yield f

    assert(result.flatMap(_.focus) === Some(200.2.asJson))
  }

  it should "fail at the top" in forAll { (j: Json, key: String) =>
    val result = HCursor.fromJson(j).field(key)

    assert(result.failed && result.history === List(CursorOp.Field(key)))
  }

  "getOrElse" should "successfully decode an existing field" in {
    val result = for {
      b <- cursor.downField("b").success
    } yield b.getOrElse[List[Boolean]]("d")(Nil)
    assert(result === Some(Right(List(true, false, true))))
  }

  it should "use the fallback if field is missing" in {
    val result = for {
      b <- cursor.downField("b").success
    } yield b.getOrElse[List[Boolean]]("z")(Nil)
    assert(result === Some(Right(Nil)))
  }

  it should "fail if the field is the wrong type" in {
    val result = for {
      b <- cursor.downField("b").success
    } yield b.getOrElse[List[Int]]("d")(Nil)
    assert(result.fold(false)(_.isLeft))
  }
}
