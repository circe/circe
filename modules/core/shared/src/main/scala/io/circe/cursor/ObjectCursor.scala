/*
 * Copyright 2024 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe.cursor

import io.circe._

private[circe] final class ObjectCursor(obj: JsonObject, val keyValue: String, val parent: HCursor, changed: Boolean)(
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
}
