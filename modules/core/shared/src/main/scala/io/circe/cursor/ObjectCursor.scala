package io.circe.cursor

import io.circe.{ ACursor, CursorOp, HCursor, Json, JsonObject }

private[circe] final class ObjectCursor(obj: JsonObject, key: String, parent: HCursor, changed: Boolean)(
  lastCursor: HCursor,
  lastOp: CursorOp
) extends HCursor(lastCursor, lastOp) {
  def value: Json = obj.toMap(key)

  def replace(newValue: Json, cursor: HCursor, op: CursorOp): HCursor =
    new ObjectCursor(obj.add(key, newValue), key, parent, true)(cursor, op)

  def addOp(cursor: HCursor, op: CursorOp): HCursor = new ObjectCursor(obj, key, parent, changed)(cursor, op)

  def lefts: Option[Vector[Json]] = None
  def rights: Option[Vector[Json]] = None

  def up: ACursor = if (!changed) parent.addOp(this, CursorOp.MoveUp) else {
    parent.replace(Json.fromJsonObject(obj), this, CursorOp.MoveUp)
  }

  def delete: ACursor = parent.replace(Json.fromJsonObject(obj.remove(key)), this, CursorOp.DeleteGoParent)

  def field(k: String): ACursor = {
    val m = obj.toMap

    if (!m.contains(k)) fail(CursorOp.Field(k)) else {
      new ObjectCursor(obj, k, parent, changed)(this, CursorOp.Field(k))
    }
  }

  def deleteGoField(k: String): ACursor = {
    val m = obj.toMap

    if (m.contains(k)) new ObjectCursor(obj.remove(key), k, parent, true)(this, CursorOp.DeleteGoField(k))
      else fail(CursorOp.DeleteGoField(k))
  }

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
}
