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

import cats.syntax.all._

/**
 * A newtype over a vector which describes a path in a JSON object from some
 * point to the root of the JSON object.
 *
 * The ''first'' element in the vector is the root of the object (if present),
 * and the ''last'' element in the vector is the starting location.
 *
 * This datatype exists so that we can store the minimal amount of
 * information required to give a nice description of some event related to
 * JSON, usually a decoding error. It permits us to avoid creating the
 * message early as much of the time the message is not needed.
 *
 * @note This type works around a number of modeling issues in the Cursors in
 *       Circe. It was created to avoid a mass deprecation on 0.14.x. For this
 *       reason it is intentionally minimal and private. A more dramatic
 *       change which will involve deprecations is planned for 0.15.x.
 */
private[circe] final case class PathToRoot private (value: Vector[PathToRoot.PathElem]) extends AnyVal {

  /**
   * A `String` representation of the path into a JSON object. For example, for
   * the JSON `{"a": [1,2]}`, a cursor pointing at `2` would yield a
   * `String`of the form `"a[1]"`.
   */
  def asPathString: String =
    PathToRoot.toPathString(this)

  def prependElem(elem: PathToRoot.PathElem): PathToRoot =
    PathToRoot(elem +: value)

  def appendElem(elem: PathToRoot.PathElem): PathToRoot =
    PathToRoot(value :+ elem)

  def +:(elem: PathToRoot.PathElem): PathToRoot =
    prependElem(elem)

  def :+(elem: PathToRoot.PathElem): PathToRoot =
    appendElem(elem)
}

private[circe] object PathToRoot {
  val empty: PathToRoot = PathToRoot(Vector.empty)

  /**
   * Used to describe the breadcrumbs back to the root of the JSON.
   */
  sealed abstract class PathElem extends Product with Serializable

  object PathElem {
    final case class ObjectKey(keyName: String) extends PathElem
    final case class ArrayIndex(index: Long) extends PathElem

    object ArrayIndex {
      def apply(value: Int): ArrayIndex =
        ArrayIndex(value.toLong)
    }
  }

  /**
   * Convert a [[PathToRoot]] into a `String` representation of a path into a
   * JSON object. For example, for the JSON `{"a": [1,2]}`, a cursor pointing
   * at `2` would yield a `String`of the form `"a[1]"`.
   */
  def toPathString(path: PathToRoot): String = {
    import PathElem._

    if (path.value.isEmpty) {
      ""
    } else {
      path.value
        .foldLeft(new StringBuilder(path.value.size * 5)) {
          case (sb, ObjectKey(keyName)) =>
            sb.append(".").append(keyName)
          case (sb, ArrayIndex(index)) =>
            sb.append("[").append(index.toString).append("]")
        }
        .toString
    }
  }

  def fromHistory(ops: List[CursorOp]): Either[String, PathToRoot] = {
    type F[A] = Either[String, A] // Kind Projector?
    val moveUpErrorString: String = "Attempt to move up above the root of the JSON."

    ops.reverse
      .foldM[F, Vector[PathElem]](Vector.empty[PathElem]) {
        // MoveLeft
        case (acc :+ PathElem.ArrayIndex(n), CursorOp.MoveLeft) if n <= 0 =>
          Left("Attempt to move beyond beginning of array in cursor history.")
        case (acc :+ PathElem.ArrayIndex(n), CursorOp.MoveLeft) =>
          Right(acc :+ PathElem.ArrayIndex(n - 1))

        // MoveRight
        case (acc :+ PathElem.ArrayIndex(n), CursorOp.MoveRight) if n === Int.MaxValue =>
          Left("Attempt to move to index > Int.MaxValue in array in cursor history.")
        case (acc :+ PathElem.ArrayIndex(n), CursorOp.MoveRight) =>
          Right(acc :+ PathElem.ArrayIndex(n + 1))

        // MoveUp
        case (acc :+ _, CursorOp.MoveUp) =>
          Right(acc)
        case (acc, CursorOp.MoveUp) =>
          Left(moveUpErrorString)

        // Field
        case (acc :+ PathElem.ObjectKey(_), CursorOp.Field(name)) =>
          Right(acc :+ PathElem.ObjectKey(name))
        case (_, CursorOp.Field(name)) =>
          Left("Attempt to move to sibling field, but cursor history didn't indicate we were in an object.")

        // DownField
        case (acc, CursorOp.DownField(name)) =>
          Right(acc :+ PathElem.ObjectKey(name))

        // DownArray
        case (acc, CursorOp.DownArray) =>
          Right(acc :+ PathElem.ArrayIndex(0))

        // DownN
        case (acc, CursorOp.DownN(n)) =>
          Right(acc :+ PathElem.ArrayIndex(n))

        // DeleteGoParent
        case (acc :+ _, CursorOp.DeleteGoParent) =>
          Right(acc)
        case (acc, CursorOp.DeleteGoParent) =>
          Left(moveUpErrorString)

        // Otherwise
        case (acc, invalid) =>
          Left(s"Invalid cursor history state: ${invalid}")
      }
      .map(PathToRoot.apply _)
  }
}
