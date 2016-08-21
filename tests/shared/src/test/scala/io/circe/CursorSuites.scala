package io.circe

import cats.instances.list._
import io.circe.tests.CursorSuite

class BasicCursorSuite extends CursorSuite[Cursor] {
  def fromJson(j: Json): Cursor = Cursor(j)
  def top(c: Cursor): Option[Json] = Some(c.top)
  def focus(c: Cursor): Option[Json] = Some(c.focus)
  def fromResult(result: Option[Cursor]): Option[Cursor] = result
}

class HCursorSuite extends CursorSuite[HCursor] {
  def fromJson(j: Json): HCursor = j.hcursor
  def top(c: HCursor): Option[Json] = Some(c.top)
  def focus(c: HCursor): Option[Json] = Some(c.focus)
  def fromResult(result: ACursor): Option[HCursor] = result.success

  "replay" should "replay history" in {
    val cursor = fromJson(j2)
    val result = cursor.downField("b").downField("d").downN(2)

    assert(
      result.focus === Some(Json.True) &&
      cursor.replay(result.history) === result
    )
  }
}

class ACursorSuite extends CursorSuite[ACursor] {
  def fromJson(j: Json): ACursor = j.hcursor.acursor
  def top(c: ACursor): Option[Json] = c.top
  def focus(c: ACursor): Option[Json] = c.focus
  def fromResult(result: ACursor): Option[ACursor] = result.success.map(_.acursor)
}
