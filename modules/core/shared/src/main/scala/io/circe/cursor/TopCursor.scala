package io.circe.cursor

import io.circe.{ ACursor, CursorOp, HCursor, Json }

private[circe] final class TopCursor(val value: Json)(
  lastCursor: HCursor,
  lastOp: CursorOp
) extends HCursor(lastCursor, lastOp) {
  def replace(newValue: Json, cursor: HCursor, op: CursorOp): HCursor = new TopCursor(newValue)(cursor, op)
  def addOp(cursor: HCursor, op: CursorOp): HCursor = new TopCursor(value)(cursor, op)

  def lefts: Option[Vector[Json]] = None
  def rights: Option[Vector[Json]] = None

  def up: ACursor = fail(CursorOp.MoveUp)
  def delete: ACursor = fail(CursorOp.DeleteGoParent)

  def left: ACursor = fail(CursorOp.MoveLeft)
  def right: ACursor = fail(CursorOp.MoveRight)
  def first: ACursor = fail(CursorOp.MoveFirst)
  def last: ACursor = fail(CursorOp.MoveLast)

  def deleteGoLeft: ACursor = fail(CursorOp.DeleteGoLeft)
  def deleteGoRight: ACursor = fail(CursorOp.DeleteGoRight)
  def deleteGoFirst: ACursor = fail(CursorOp.DeleteGoFirst)
  def deleteGoLast: ACursor = fail(CursorOp.DeleteGoLast)
  def deleteLefts: ACursor = fail(CursorOp.DeleteLefts)
  def deleteRights: ACursor = fail(CursorOp.DeleteRights)

  def setLefts(x: Vector[Json]): ACursor = fail(CursorOp.SetLefts(x))
  def setRights(x: Vector[Json]): ACursor = fail(CursorOp.SetRights(x))

  def field(k: String): ACursor = fail(CursorOp.Field(k))
  def deleteGoField(k: String): ACursor = fail(CursorOp.DeleteGoField(k))
}
