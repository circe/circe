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
