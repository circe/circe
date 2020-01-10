package io.circe.cursor

import io.circe.{ ACursor, CursorOp, HCursor, Json }

private[circe] final class ArrayCursor(values: Vector[Json], index: Int, parent: HCursor, changed: Boolean)(
  lastCursor: HCursor,
  lastOp: CursorOp
) extends HCursor(lastCursor, lastOp) {
  def value: Json = values(index)

  private[this] def valuesExcept: Vector[Json] = values.take(index) ++ values.drop(index + 1)

  def replace(newValue: Json, cursor: HCursor, op: CursorOp): HCursor =
    new ArrayCursor(values.updated(index, newValue), index, parent, true)(cursor, op)

  def addOp(cursor: HCursor, op: CursorOp): HCursor =
    new ArrayCursor(values, index, parent, changed)(cursor, op)

  def up: ACursor =
    if (!changed) parent.addOp(this, CursorOp.MoveUp)
    else {
      parent.replace(Json.fromValues(values), this, CursorOp.MoveUp)
    }

  def delete: ACursor = parent.replace(Json.fromValues(valuesExcept), this, CursorOp.DeleteGoParent)

  def left: ACursor = if (index == 0) fail(CursorOp.MoveLeft)
  else {
    new ArrayCursor(values, index - 1, parent, changed)(this, CursorOp.MoveLeft)
  }

  def right: ACursor = if (index == values.size - 1) fail(CursorOp.MoveRight)
  else {
    new ArrayCursor(values, index + 1, parent, changed)(this, CursorOp.MoveRight)
  }

  def field(k: String): ACursor = fail(CursorOp.Field(k))
}
