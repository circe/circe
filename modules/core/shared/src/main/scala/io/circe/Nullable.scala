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

package io.circe

sealed abstract class Nullable[+A] extends Product with Serializable {

  def isNull: Boolean
  def isUndefined: Boolean
  def toOption: Option[A]

}

object Nullable {

  case object Undefined extends Nullable[Nothing] {
    def isNull: Boolean = false
    def isUndefined: Boolean = true
    def toOption: Option[Nothing] = None
  }

  case object Null extends Nullable[Nothing] {
    def isNull: Boolean = true
    def isUndefined: Boolean = false
    def toOption: Option[Nothing] = None
  }

  case class Value[A](value: A) extends Nullable[A] {
    def isNull: Boolean = false
    def isUndefined: Boolean = false
    def toOption: Option[A] = Some(value)
  }

}
