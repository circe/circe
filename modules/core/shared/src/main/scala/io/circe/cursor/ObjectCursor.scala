package io.circe.cursor

import io.circe.{ ACursor, CursorOp, HCursor, Json, JsonObject }

private[circe] final class ObjectCursor(obj: JsonObject, keyValue: String, parent: HCursor, changed: Boolean)(
  lastCursor: HCursor,
  lastOp: CursorOp
) extends HCursor(lastCursor, lastOp) {
  def value: Json = obj.applyUnsafe(keyValue)
  override def index: Option[Int] = None
  override def key: Option[String] = Some(keyValue)

  def replace(newValue: Json, cursor: HCursor, op: CursorOp): HCursor =
    new ObjectCursor(obj.add(keyValue, newValue), keyValue, parent, true)(cursor, op)

  def addOp(cursor: HCursor, op: CursorOp): HCursor = new ObjectCursor(obj, keyValue, parent, changed)(cursor, op)

  def up: ACursor = if (!changed) parent.addOp(this, CursorOp.MoveUp)
  else {
    parent.replace(Json.fromJsonObject(obj), this, CursorOp.MoveUp)
  }

  def delete: ACursor = parent.replace(Json.fromJsonObject(obj.remove(keyValue)), this, CursorOp.DeleteGoParent)

  def field(k: String): ACursor =
    if (!obj.contains(k)) fail(CursorOp.Field(k))
    else {
      new ObjectCursor(obj, k, parent, changed)(this, CursorOp.Field(k))
    }

  def left: ACursor = fail(CursorOp.MoveLeft)
  def right: ACursor = fail(CursorOp.MoveRight)
  def first: ACursor = fail(CursorOp.MoveFirst)
}
