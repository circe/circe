package io.circe.cursor

import io.circe.{ ACursor, CursorOp, HCursor, Json }

private[circe] final class ArrayCursor(values: Vector[Json], indexValue: Int, parent: HCursor, changed: Boolean)(
  lastCursor: HCursor,
  lastOp: CursorOp
) extends HCursor(lastCursor, lastOp) {
  def value: Json = values(indexValue)
  override def index: Option[Int] = Some(indexValue)
  override def key: Option[String] = None

  private[this] def valuesExcept: Vector[Json] = values.take(indexValue) ++ values.drop(indexValue + 1)

  def replace(newValue: Json, cursor: HCursor, op: CursorOp): HCursor =
    new ArrayCursor(values.updated(indexValue, newValue), indexValue, parent, true)(cursor, op)

  def addOp(cursor: HCursor, op: CursorOp): HCursor =
    new ArrayCursor(values, indexValue, parent, changed)(cursor, op)

  def up: ACursor =
    if (!changed) parent.addOp(this, CursorOp.MoveUp)
    else {
      parent.replace(Json.fromValues(values), this, CursorOp.MoveUp)
    }

  def delete: ACursor = parent.replace(Json.fromValues(valuesExcept), this, CursorOp.DeleteGoParent)

  def left: ACursor = if (indexValue == 0) fail(CursorOp.MoveLeft)
  else {
    new ArrayCursor(values, indexValue - 1, parent, changed)(this, CursorOp.MoveLeft)
  }

  def right: ACursor = if (indexValue == values.size - 1) fail(CursorOp.MoveRight)
  else {
    new ArrayCursor(values, indexValue + 1, parent, changed)(this, CursorOp.MoveRight)
  }

  def field(k: String): ACursor = fail(CursorOp.Field(k))
}
