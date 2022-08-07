package io.circe

import cats._
import cats.kernel.Eq
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
abstract class ACursor(private val evalLastCursor: Eval[HCursor], private val lastOp: CursorOp) extends Serializable {

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
  def history: List[CursorOp]

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
  def root: HCursor

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
  def index: Option[Int]

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
  def key: Option[String]

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

  private[circe] def pathToRoot: PathToRoot

  /**
   * Creates a JavaScript-style path string, e.g. ".foo.bar[3]".
   */
  final def pathString: String =
    pathToRoot.asPathString

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
  def replayOne(op: CursorOp): ACursor

  /**
   * Replay history (a list of operations in reverse "chronological" order) against this cursor.
   *
   * @group Utilities
   */
  def replay(history: List[CursorOp]): ACursor

  protected final def lastCursor: HCursor =
    evalLastCursor.value
}

object ACursor {
  private[this] val jsonOptionEq: Eq[Option[Json]] = cats.kernel.instances.option.catsKernelStdEqForOption(Json.eqJson)

  implicit val eqACursor: Eq[ACursor] =
    Eq.instance((a, b) => jsonOptionEq.eqv(a.focus, b.focus) && CursorOp.eqCursorOpList.eqv(a.history, b.history))

  def fromCursor(cursor: Cursor): ACursor =
    cursor match {
      case cursor: Cursor.SuccessCursor =>
        HCursor.fromCursor(cursor)
      case cursor =>
        new FailedCursor(
          Eval.later(cursor.lastCursor.fold(null: HCursor)(HCursor.fromCursor)),
          cursor.lastOp.getOrElse(null)
        )
    }
}
