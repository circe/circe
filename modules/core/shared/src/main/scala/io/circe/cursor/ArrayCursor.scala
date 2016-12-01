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
    if (!changed) parent.addOp(this, CursorOp.MoveUp) else {
      parent.replace(Json.fromValues(values), this, CursorOp.MoveUp)
    }

  def delete: ACursor = parent.replace(Json.fromValues(valuesExcept), this, CursorOp.DeleteGoParent)

  def lefts: Option[Vector[Json]] = Some(values.take(index).reverse)
  def rights: Option[Vector[Json]] = Some(values.drop(index + 1))

  def left: ACursor = if (index == 0) fail(CursorOp.MoveLeft) else {
    new ArrayCursor(values, index - 1, parent, changed)(this, CursorOp.MoveLeft)
  }

  def right: ACursor = if (index == values.size - 1) fail(CursorOp.MoveRight) else {
    new ArrayCursor(values, index + 1, parent, changed)(this, CursorOp.MoveRight)
  }

  def first: ACursor = new ArrayCursor(values, 0, parent, changed)(this, CursorOp.MoveFirst)
  def last: ACursor = new ArrayCursor(values, values.size - 1, parent, changed)(this, CursorOp.MoveLast)

  def deleteGoLeft: ACursor = if (index == 0) fail(CursorOp.DeleteGoLeft) else {
    new ArrayCursor(valuesExcept, index - 1, parent, true)(this, CursorOp.DeleteGoLeft)
  }

  def deleteGoRight: ACursor = if (index == values.size - 1) fail(CursorOp.DeleteGoRight) else {
    new ArrayCursor(valuesExcept, index, parent, true)(this, CursorOp.DeleteGoRight)
  }

  def deleteGoFirst: ACursor = if (values.size == 1) fail(CursorOp.DeleteGoFirst) else {
    new ArrayCursor(valuesExcept, 0, parent, true)(this, CursorOp.DeleteGoFirst)
  }

  def deleteGoLast: ACursor = if (values.size == 1) fail(CursorOp.DeleteGoLast) else {
    new ArrayCursor(valuesExcept, values.size - 2, parent, true)(this, CursorOp.DeleteGoLast)
  }

  def deleteLefts: ACursor = new ArrayCursor(values.drop(index), 0, parent, index != 0)(this, CursorOp.DeleteLefts)
  def deleteRights: ACursor =
    new ArrayCursor(values.take(index + 1), index, parent, index < values.size)(this, CursorOp.DeleteRights)

  def setLefts(js: Vector[Json]): ACursor =
    new ArrayCursor(js.reverse ++ values.drop(index), js.size, parent, true)(this, CursorOp.SetLefts(js))

  def setRights(js: Vector[Json]): ACursor =
    new ArrayCursor(values.take(index + 1) ++ js, index, parent, true)(this, CursorOp.SetRights(js))

  def field(k: String): ACursor = fail(CursorOp.Field(k))
  def deleteGoField(k: String): ACursor = fail(CursorOp.DeleteGoField(k))
}
