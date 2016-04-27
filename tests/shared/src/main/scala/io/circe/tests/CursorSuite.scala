package io.circe.tests

import algebra.Eq
import io.circe.{ GenericCursor, Json }
import io.circe.syntax._

abstract class CursorSuite[C <: GenericCursor[C]](implicit
  eq: Eq[C],
  M: C#M[List]
) extends CirceSuite {
  def fromJson(j: Json): C { type M[x[_]] = C#M[x] }
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

  "focus" should "return the JSON value in a newly created cursor" in forAll { (j: Json) =>
    assert(focus(fromJson(j)) === Some(j))
  }

  "top" should "return from navigation into an object" in forAll { (j: Json) =>
    val c = fromJson(j)

    val intoObject = for {
      fields  <- c.fields
      first   <- fields.headOption
      atFirst <- fromResult(c.downField(first))
    } yield atFirst

    assert(intoObject.forall(atFirst => top(atFirst) === Some(j)))
  }

  it should "return from navigation into an array" in forAll { (j: Json) =>
    assert(fromResult(fromJson(j).downArray).forall(atFirst => top(atFirst) === Some(j)))
  }

  "up" should "undo navigation into an object" in forAll { (j: Json) =>
    val c = fromJson(j)

    val intoObject = for {
      fields  <- c.fields
      first   <- fields.headOption
      atFirst <- fromResult(c.downField(first))
    } yield atFirst

    assert(intoObject.forall(atFirst => fromResult(atFirst.up).flatMap(focus) === Some(j)))
  }

  it should "undo navigation into an array" in forAll { (j: Json) =>
    assert(
      fromResult(fromJson(j).downArray).forall(atFirst =>
        fromResult(atFirst.up).flatMap(focus) === Some(j)
      )
    )
  }

  "withFocus" should "have no effect when given the identity function" in forAll { (j: Json) =>
    assert(focus(fromJson(j).withFocus(identity)) === Some(j))
  }

  it should "support adding an element to an array" in {
    val result = fromResult(cursor.downField("a")).map(
      _.withFocus(j =>
        j.asArray.fold(j)(a => Json.fromValues(0.asJson :: a))
      )
    )

    assert(result.flatMap(top) === Some(j2))
  }

  "withFocusM" should "lift a value into a List" in forAll { (j: Json) =>
    assert(focus(fromJson(j).withFocusM[List](List(_))(M).head) === Some(j))
  }

  "delete" should "remove a value from an object" in {
    val result = fromResult(cursor.downField("b")).flatMap(c => fromResult(c.delete))

    assert(result.flatMap(top) === Some(j4))
  }

  it should "remove a value from an array" in forAll { (h: Json, t: List[Json]) =>
    val result = for {
      f <- fromResult(fromJson(Json.fromValues(h :: t)).downArray)
      u <- fromResult(f.delete)
    } yield u

    assert(result.flatMap(focus) === Some(Json.fromValues(t)))
  }

  "set" should "replace an element" in {
    val result = fromResult(cursor.downField("b")).map(_.set(10.asJson))

    assert(result.flatMap(top) === Some(j3))
  }

  "lefts" should "return the expected values" in {
    val result = for {
      c <- fromResult(cursor.downField("a"))
      a <- fromResult(c.downN(3))
      l <- a.lefts
    } yield l

    assert(result === Some(List(3.asJson, 2.asJson, 1.asJson)))
  }

  "rights" should "return the expected values" in {
    val result = for {
      c <- fromResult(cursor.downField("a"))
      a <- fromResult(c.downN(3))
      l <- a.rights
    } yield l

    assert(result === Some(List(5.asJson)))
  }

  "fieldSet" should "return the expected values" in {
    assert(fromJson(j1).fieldSet.map(_.toList.sorted) === Some(List("a", "b", "c")))
  }

  "fields" should "return the expected values" in {
    assert(fromJson(j1).fields === Some(List("a", "b", "c")))
  }

  "left" should "successfully select an existing value" in {
    val result = for {
      c <- fromResult(cursor.downField("a"))
      a <- fromResult(c.downN(3))
      l <- fromResult(a.left)
    } yield l

    assert(result.flatMap(focus) === Some(3.asJson))
  }

  it should "fail to select a value that doesn't exist" in {
    val result = for {
      c <- fromResult(cursor.downField("b"))
      l <- fromResult(c.left)
    } yield l

    assert(result.flatMap(focus) === None)
  }

  "right" should "successfully select an existing value" in {
    val result = for {
      c <- fromResult(cursor.downField("a"))
      a <- fromResult(c.downN(3))
      r <- fromResult(a.right)
    } yield r

    assert(result.flatMap(focus) === Some(5.asJson))
  }

  it should "fail to select a value that doesn't exist" in {
    val result = for {
      c <- fromResult(cursor.downField("b"))
      r <- fromResult(c.right)
    } yield r

    assert(result.flatMap(focus) === None)
  }

  "first" should "successfully select an existing value" in {
    val result = for {
      c <- fromResult(cursor.downField("a"))
      a <- fromResult(c.downN(3))
      f <- fromResult(a.first)
    } yield f

    assert(result.flatMap(focus) === Some(1.asJson))
  }

  it should "fail to select a value that doesn't exist" in {
    val result = for {
      c <- fromResult(cursor.downField("b"))
      f <- fromResult(c.first)
    } yield f

    assert(result.flatMap(focus) === None)
  }

  "last" should "successfully select an existing value" in {
    val result = for {
      c <- fromResult(cursor.downField("a"))
      a <- fromResult(c.downN(3))
      l <- fromResult(a.last)
    } yield l

    assert(result.flatMap(focus) === Some(5.asJson))
  }

  it should "fail to select a value that doesn't exist" in {
    val result = for {
      c <- fromResult(cursor.downField("b"))
      l <- fromResult(c.last)
    } yield l

    assert(result.flatMap(focus) === None)
  }

  "leftAt" should "successfully select an existing value" in {
    val result = for {
      c <- fromResult(cursor.downField("a"))
      a <- fromResult(c.downN(3))
      l <- fromResult(a.leftAt(_.as[Int].exists(_ == 1)))
    } yield l

    assert(result.flatMap(focus) === Some(1.asJson))
  }

  it should "fail to select a value that doesn't exist" in {
    val result = for {
      c <- fromResult(cursor.downField("b"))
      l <- fromResult(c.leftAt(_.as[Int].exists(_ == 1)))
    } yield l

    assert(result.flatMap(focus) === None)
  }

  "rightAt" should "successfully select an existing value" in {
    val result = for {
      c <- fromResult(cursor.downField("a"))
      a <- fromResult(c.downN(3))
      r <- fromResult(a.rightAt(_.as[Int].exists(_ == 5)))
    } yield r

    assert(result.flatMap(focus) === Some(5.asJson))
  }

  it should "fail to select a value that doesn't exist" in {
    val result = for {
      c <- fromResult(cursor.downField("b"))
      r <- fromResult(c.rightAt(_.as[Int].exists(_ == 5)))
    } yield r

    assert(result.flatMap(focus) === None)
  }

  "field" should "successfully select an existing value" in {
    val result = for {
      c <- fromResult(cursor.downField("c"))
      e <- fromResult(c.downField("e"))
      f <- fromResult(e.field("f"))
    } yield f

    assert(result.flatMap(focus) === Some(200.2.asJson))
  }

  "deleteGoLeft" should "remove the current value and move appropriately" in {
    val result = for {
      c <- fromResult(cursor.downField("a"))
      a <- fromResult(c.downN(3))
      l <- fromResult(a.deleteGoLeft)
      u <- fromResult(l.up)
      lf <- focus(l)
      uf <- focus(u)
    } yield (lf, uf)

    assert(
      result.map(_._1) === Some(3.asJson) &&
      result.map(_._2) === Some(List(1, 2, 3, 5).asJson)
    )
  }

  "deleteGoRight" should "remove the current value and move appropriately" in {
    val result = for {
      c <- fromResult(cursor.downField("a"))
      a <- fromResult(c.downN(3))
      l <- fromResult(a.deleteGoRight)
      u <- fromResult(l.up)
      lf <- focus(l)
      uf <- focus(u)
    } yield (lf, uf)

    assert(
      result.map(_._1) === Some(5.asJson) &&
      result.map(_._2) === Some(List(1, 2, 3, 5).asJson)
    )
  }

  "deleteGoFirst" should "remove the current value and move appropriately" in {
    val result = for {
      c <- fromResult(cursor.downField("a"))
      a <- fromResult(c.downN(3))
      l <- fromResult(a.deleteGoFirst)
      u <- fromResult(l.up)
      lf <- focus(l)
      uf <- focus(u)
    } yield (lf, uf)

    assert(
      result.map(_._1) === Some(1.asJson) &&
      result.map(_._2) === Some(List(1, 2, 3, 5).asJson)
    )
  }

  "deleteGoLast" should "remove the current value and move appropriately" in {
    val result = for {
      c <- fromResult(cursor.downField("a"))
      a <- fromResult(c.downN(2))
      l <- fromResult(a.deleteGoLast)
      u <- fromResult(l.up)
      lf <- focus(l)
      uf <- focus(u)
    } yield (lf, uf)

    assert(
      result.map(_._1) === Some(5.asJson) &&
      result.map(_._2) === Some(List(1, 2, 4, 5).asJson)
    )
  }

  "deleteLefts" should "remove the specified values" in {
    val result = for {
      c <- fromResult(cursor.downField("a"))
      a <- fromResult(c.downN(3))
      l <- fromResult(a.deleteLefts)
      u <- fromResult(l.up)
      lf <- focus(l)
      uf <- focus(u)
    } yield (lf, uf)

    assert(
      result.map(_._1) === Some(4.asJson) &&
      result.map(_._2) === Some(List(4, 5).asJson)
    )
  }

  "deleteRights" should "remove the specified values" in {
    val result = for {
      c <- fromResult(cursor.downField("a"))
      a <- fromResult(c.downN(3))
      l <- fromResult(a.deleteRights)
      u <- fromResult(l.up)
      lf <- focus(l)
      uf <- focus(u)
    } yield (lf, uf)

    assert(
      result.map(_._1) === Some(4.asJson) &&
      result.map(_._2) === Some(List(1, 2, 3, 4).asJson)
    )
  }

  "deleteGoField" should "remove the current value and move appropriately" in {
    val result = for {
      c <- fromResult(cursor.downField("c"))
      a <- fromResult(c.downField("e"))
      l <- fromResult(a.deleteGoField("f"))
      u <- fromResult(l.up)
      lf <- focus(l)
      uf <- focus(u)
    } yield (lf, uf)

    assert(
      result.map(_._1) === Some(200.2.asJson) &&
      result.map(_._2) === Some(Map("f" -> 200.2).asJson)
    )
  }

  "setLefts" should "replace the specified values" in {
    val result = for {
      c <- fromResult(cursor.downField("a"))
      a <- fromResult(c.downN(3))
      l <- fromResult(a.setLefts(List(100.asJson, 101.asJson)))
      u <- fromResult(l.up)
      lf <- focus(l)
      uf <- focus(u)
    } yield (lf, uf)

    assert(
      result.map(_._1) === Some(4.asJson) &&
      result.map(_._2) === Some(List(101, 100, 4, 5).asJson)
    )
  }

  "setRights" should "replace the specified values" in {
    val result = for {
      c <- fromResult(cursor.downField("a"))
      a <- fromResult(c.downN(3))
      l <- fromResult(a.setRights(List(100.asJson, 101.asJson)))
      u <- fromResult(l.up)
      lf <- focus(l)
      uf <- focus(u)
    } yield (lf, uf)

    assert(
      result.map(_._1) === Some(4.asJson) &&
      result.map(_._2) === Some(List(1, 2, 3, 4, 100, 101).asJson)
    )
  }
}
