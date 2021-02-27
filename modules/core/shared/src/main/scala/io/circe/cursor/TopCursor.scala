package io.circe.cursor

import io.circe.{ ACursor, CursorOp, HCursor, Json }

private[circe] final class TopCursor(val value: Json)(
  lastCursor: HCursor,
  lastOp: CursorOp
) extends HCursor(lastCursor, lastOp) {
  override def index: Option[Int] = None
  override def key: Option[String] = None

  def replace(newValue: Json, cursor: HCursor, op: CursorOp): HCursor = new TopCursor(newValue)(cursor, op)
  def addOp(cursor: HCursor, op: CursorOp): HCursor = new TopCursor(value)(cursor, op)

  def up: ACursor = fail(CursorOp.MoveUp)
  def delete: ACursor = fail(CursorOp.DeleteGoParent)

  def left: ACursor = fail(CursorOp.MoveLeft)
  def right: ACursor = fail(CursorOp.MoveRight)

  def field(k: String): ACursor = fail(CursorOp.Field(k))
}
