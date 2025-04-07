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

import cats.Applicative
import cats.kernel.Eq
import io.circe.cursor._

import java.io.Serializable
import scala.annotation.tailrec

/**
 * A zipper that represents a position in a JSON document and supports navigation and modification.
 *
 * The `focus` represents the current position of the cursor; it may be updated with `withFocus` or
 * changed using navigation methods like `left` and `right`.
 *
 * @groupname Utilities Miscellaneous utilities
 * @groupprio Utilities 0
 * @groupname Access Access and navigation
 * @groupprio Access 1
 * @groupname Modification Modification
 * @groupprio Modification 2
 * @groupname ArrayAccess Array access
 * @groupprio ArrayAccess 3
 * @groupname ObjectAccess Object access
 * @groupprio ObjectAccess 4
 * @groupname ArrayNavigation Array navigation
 * @groupprio ArrayNavigation 5
 * @groupname ObjectNavigation Object navigation
 * @groupprio ObjectNavigation 6
 * @groupname ArrayModification Array modification
 * @groupprio ArrayModification 7
 * @groupname ObjectModification Object modification
 * @groupprio ObjectModification 8
 * @groupname Decoding Decoding
 * @groupprio Decoding 9
 *
 * @author Travis Brown
 */
abstract class ACursor(private val lastCursor: HCursor, private val lastOp: CursorOp) extends Serializable {

  /**
   * The current location in the document.
   *
   * @group Access
   */
  def focus: Option[Json]

  /**
   * The operations that have been performed so far.
   *
   * @group Decoding
   */
  final def history: List[CursorOp] = {
    var next = this
    val builder = List.newBuilder[CursorOp]

    while (next.ne(null)) {
      if (next.lastOp.ne(null)) {
        builder += next.lastOp
      }
      next = next.lastCursor
    }

    builder.result()
  }

  /**
   * Indicate whether this cursor represents the result of a successful
   * operation.
   *
   * @group Decoding
   */
  def succeeded: Boolean

  /**
   * Indicate whether this cursor represents the result of an unsuccessful
   * operation.
   *
   * @group Decoding
   */
  final def failed: Boolean = !succeeded

  /**
   * Return the cursor as an [[HCursor]] if it was successful.
   *
   * @group Decoding
   */
  def success: Option[HCursor]

  /**
   * Return to the root of the document.
   *
   * @group Access
   */
  def top: Option[Json]

  /**
   * Return the cursor to the root of the document.
   *
   * @group Access
   */
  def root: HCursor = null

  /**
   * Modify the focus using the given function.
   *
   * @group Modification
   */
  def withFocus(f: Json => Json): ACursor

  /**
   * Modify the focus in a context using the given function.
   *
   * @group Modification
   */
  def withFocusM[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[ACursor]

  /**
   * Replace the focus.
   *
   * @group Modification
   */
  final def set(j: Json): ACursor = withFocus(_ => j)

  /**
   * If the focus is a JSON array, return its elements.
   *
   * @group ArrayAccess
   */
  def values: Option[Iterable[Json]]

  /**
   * If the focus is a value in a JSON array, return the key.
   *
   * @group ArrayAccess
   */
  def index: Option[Int] = None

  /**
   * If the focus is a JSON object, return its field names in their original order.
   *
   * @group ObjectAccess
   */
  def keys: Option[Iterable[String]]

  /**
   * If the focus is a value in a JSON object, return the key.
   *
   * @group ObjectAccess
   */
  def key: Option[String] = None

  /**
   * Delete the focus and move to its parent.
   *
   * @group Modification
   */
  def delete: ACursor

  /**
   * Move the focus to the parent.
   *
   * @group Access
   */
  def up: ACursor

  /**
   * If the focus is an element in a JSON array, move to the left.
   *
   * @group ArrayNavigation
   */
  def left: ACursor

  /**
   * If the focus is an element in a JSON array, move to the right.
   *
   * @group ArrayNavigation
   */
  def right: ACursor

  /**
   * If the focus is a JSON array, move to its first element.
   *
   * @group ArrayNavigation
   */
  def downArray: ACursor

  /**
   * If the focus is a JSON array, move to the element at the given index.
   *
   * @group ArrayNavigation
   */
  def downN(n: Int): ACursor

  /**
   * If the focus is a value in a JSON object, move to a sibling with the given key.
   *
   * @group ObjectNavigation
   */
  def field(k: String): ACursor

  /**
   * If the focus is a JSON object, move to the value of the given key.
   *
   * @group ObjectNavigation
   */
  def downField(k: String): ACursor

  /**
   * Navigates down into the specified fields of a JSON object sequentially.
   *
   * @param k  The first field name to navigate into.
   * @param ks Additional field names to navigate into, in sequence.
   * @return An updated cursor focused on the final field in the sequence, or a failed cursor if any field is not found.
   */
  final def downFields(k: String, ks: String*): ACursor = {
    ks.foldLeft(this.downField(k))(_.downField(_))
  }

  private[circe] final def pathToRoot: PathToRoot = {
    import PathToRoot._

    // Move to the last cursor or to the last cursor`s parent
    // if cursor is intermediate. Empty the last cursor should call
    // termination of cursors`s traverse
    def lastCursorParentOrLastCursor(cursor: ACursor): ACursor =
      cursor.lastCursor match {
        case lastCursor: ArrayCursor =>
          lastCursor.parent
        case lastCursor =>
          lastCursor
      }

    @tailrec
    def loop(cursor: ACursor, acc: PathToRoot): PathToRoot =
      if (cursor eq null) {
        acc
      } else {
        if (cursor.failed) {
          // If the cursor is in a failed state, we lose context on what the
          // attempted last position was. Since we usually want to know this
          // for error reporting, we use the lastOp to attempt to recover that
          // state. We only care about operations which imply a path to the
          // root, such as a field selection.
          cursor.lastOp match {
            case CursorOp.Field(field) =>
              loop(lastCursorParentOrLastCursor(cursor), PathElem.ObjectKey(field) +: acc)
            case CursorOp.DownField(field) =>
              // We tried to move down, and then that failed, so the field was
              // missing.
              loop(lastCursorParentOrLastCursor(cursor), PathElem.ObjectKey(field) +: acc)
            case CursorOp.DownArray =>
              // We tried to move into an array, but it must have been empty.
              loop(lastCursorParentOrLastCursor(cursor), PathElem.ArrayIndex(0) +: acc)

            case CursorOp.DownN(n) =>
              // We tried to move into an array at index N, but there was no
              // element there.
              loop(lastCursorParentOrLastCursor(cursor), PathElem.ArrayIndex(n) +: acc)
            case CursorOp.MoveLeft =>
              // We tried to move to before the start of the array.
              loop(lastCursorParentOrLastCursor(cursor), PathElem.ArrayIndex(-1) +: acc)
            case CursorOp.MoveRight =>
              cursor.lastCursor match {
                case lastCursor: ArrayCursor =>
                  // We tried to move to past the end of the array. Longs are
                  // used here for the very unlikely case that we tried to
                  // move past Int.MaxValue which shouldn't be representable.
                  loop(lastCursor.parent, PathElem.ArrayIndex(lastCursor.indexValue.toLong + 1L) +: acc)
                case _ =>
                  // Invalid state, fix in 0.15.x, skip for now.
                  loop(cursor.lastCursor, acc)
              }
            case _ =>
              // CursorOp.MoveUp or CursorOp.DeleteGoParent, both are move up
              // events.
              //
              // Recalling we are in a failed branch here, this should only
              // fail if we are already at the top of the tree or if the
              // cursor state is broken (will be fixed on 0.15.x), in either
              // case this is the only valid action to take.
              loop(cursor.lastCursor, acc)
          }
        } else {
          cursor match {
            case cursor: ArrayCursor =>
              loop(cursor.parent, PathElem.ArrayIndex(cursor.indexValue) +: acc)
            case cursor: ObjectCursor =>
              loop(cursor.parent, PathElem.ObjectKey(cursor.keyValue) +: acc)
            case cursor: TopCursor =>
              acc
            case _ =>
              // Skip unknown cursor type. Maybe we should seal this?
              loop(cursor.lastCursor, acc)
          }
        }
      }

    loop(this, PathToRoot.empty)
  }

  /**
   * Creates a JavaScript-style path string, e.g. ".foo.bar[3]".
   */
  final def pathString: String =
    PathToRoot.toPathString(pathToRoot)

  /**
   * Attempt to decode the focus as an `A`.
   *
   * @group Decoding
   */
  final def as[A](implicit d: Decoder[A]): Decoder.Result[A] = d.tryDecode(this)

  /**
   * Attempt to decode the value at the given key in a JSON object as an `A`.
   *
   * @group Decoding
   */
  final def get[A](k: String)(implicit d: Decoder[A]): Decoder.Result[A] = downField(k).as[A]

  /**
   * Attempt to decode the value at the given key in a JSON object as an `A`.
   * If the field `k` is missing, then use the `fallback` instead.
   *
   * @group Decoding
   */
  final def getOrElse[A](k: String)(fallback: => A)(implicit d: Decoder[A]): Decoder.Result[A] =
    get[Option[A]](k) match {
      case Right(Some(a)) => Right(a)
      case Right(None)    => Right(fallback)
      case l @ Left(_)    => l.asInstanceOf[Decoder.Result[A]]
    }

  /**
   * Replay an operation against this cursor.
   *
   * @group Utilities
   */
  final def replayOne(op: CursorOp): ACursor = op match {
    case CursorOp.MoveLeft       => left
    case CursorOp.MoveRight      => right
    case CursorOp.MoveUp         => up
    case CursorOp.Field(k)       => field(k)
    case CursorOp.DownField(k)   => downField(k)
    case CursorOp.DownArray      => downArray
    case CursorOp.DownN(n)       => downN(n)
    case CursorOp.DeleteGoParent => delete
  }

  /**
   * Replay history (a list of operations in reverse "chronological" order) against this cursor.
   *
   * @group Utilities
   */
  final def replay(history: List[CursorOp]): ACursor = history.foldRight(this)((op, c) => c.replayOne(op))
}

object ACursor {
  private[this] val jsonOptionEq: Eq[Option[Json]] = cats.kernel.instances.option.catsKernelStdEqForOption(Json.eqJson)

  implicit val eqACursor: Eq[ACursor] =
    Eq.instance((a, b) => jsonOptionEq.eqv(a.focus, b.focus) && CursorOp.eqCursorOpList.eqv(a.history, b.history))
}
