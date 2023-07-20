/*
 * Copyright 2023 circe
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

package io.circe

import cats.Applicative

final class FailedCursor(lastCursor: HCursor, lastOp: CursorOp) extends ACursor(lastCursor, lastOp) {

  /**
   * Indicates whether the last operation failed because the type of the focus
   * was wrong.
   */
  def incorrectFocus: Boolean =
    (lastOp.requiresObject && !lastCursor.value.isObject) || (lastOp.requiresArray && !lastCursor.value.isArray)

  /**
   * Indicates whether the last operation failed because of a missing field.
   */
  def missingField: Boolean =
    lastOp match {
      case _: CursorOp.Field | _: CursorOp.DownField => true
      case _                                         => false
    }

  def succeeded: Boolean = false
  def success: Option[HCursor] = None

  def focus: Option[Json] = None
  def top: Option[Json] = None
  override def root: HCursor = lastCursor.root

  def withFocus(f: Json => Json): ACursor = this
  def withFocusM[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[ACursor] = F.pure(this)

  def values: Option[Iterable[Json]] = None
  override def index: Option[Int] = None
  def keys: Option[Iterable[String]] = None
  override def key: Option[String] = None

  def downArray: ACursor = this
  def downField(k: String): ACursor = this
  def downN(n: Int): ACursor = this
  def up: ACursor = this

  def left: ACursor = this
  def right: ACursor = this
  def last: ACursor = this

  def delete: ACursor = this

  def field(k: String): ACursor = this
}
