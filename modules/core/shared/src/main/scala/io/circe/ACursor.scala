package io.circe

import cats.Applicative
import cats.kernel.Eq
import java.io.Serializable
import scala.collection.immutable.Set

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
   * If the focus is a JSON array, return the elements to the left.
   *
   * @group ArrayAccess
   */
  def lefts: Option[Vector[Json]]

  /**
   * If the focus is a JSON array, return the elements to the right.
   *
   * @group ArrayAccess
   */
  def rights: Option[Vector[Json]]

  /**
   * If the focus is a JSON array, return its elements.
   *
   * @group ObjectAccess
   */
  def values: Option[Iterable[Json]]

  /**
   * If the focus is a JSON object, return its field names in a set.
   *
   * @group ObjectAccess
   */
  @deprecated("Use keys.map(_.toSet)", "0.9.0")
  def fieldSet: Option[Set[String]] = keys.map(_.toSet)

  /**
   * If the focus is a JSON object, return its field names in their original order.
   *
   * @group ObjectAccess
   */
  def keys: Option[Iterable[String]]

  /**
   * If the focus is a JSON object, return its field names in their original order.
   *
   * @group ObjectAccess
   */
  @deprecated("Use keys", "0.9.0")
  final def fields: Option[Iterable[String]] = keys

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
   * If the focus is an element in a JSON array, move to the first element.
   *
   * @group ArrayNavigation
   */
  def first: ACursor

  /**
   * If the focus is an element in a JSON array, move to the last element.
   *
   * @group ArrayNavigation
   */
  def last: ACursor

  /**
   * If the focus is an element in JSON array, move to the left the given number of times.
   *
   * A negative value will move the cursor right.
   *
   * @group ArrayNavigation
   */
  def leftN(n: Int): ACursor

  /**
   * If the focus is an element in JSON array, move to the right the given number of times.
   *
   * A negative value will move the cursor left.
   *
   * @group ArrayNavigation
   */
  def rightN(n: Int): ACursor

  /**
   * If the focus is an element in a JSON array, move to the left until the given predicate matches
   * the new focus.
   *
   * @group ArrayNavigation
   */
  def leftAt(p: Json => Boolean): ACursor

  /**
   * If the focus is an element in a JSON array, move to the right until the given predicate matches
   * the new focus.
   *
   * @group ArrayNavigation
   */
  def rightAt(p: Json => Boolean): ACursor

  /**
   * If the focus is an element in a JSON array, find the first element at or to its right that
   * matches the given predicate.
   *
   * @group ArrayNavigation
   */
  def find(p: Json => Boolean): ACursor

  /**
   * If the focus is a JSON array, move to its first element.
   *
   * @group ArrayNavigation
   */
  def downArray: ACursor

  /**
   * If the focus is a JSON array, move to the first element that satisfies the given predicate.
   *
   * @group ArrayNavigation
   */
  def downAt(p: Json => Boolean): ACursor

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
   * Delete the focus and move to the left in a JSON array.
   *
   * @group ArrayModification
   */
  def deleteGoLeft: ACursor

  /**
   * Delete the focus and move to the right in a JSON array.
   *
   * @group ArrayModification
   */
  def deleteGoRight: ACursor

  /**
   * Delete the focus and move to the first element in a JSON array.
   *
   * @group ArrayModification
   */
  def deleteGoFirst: ACursor

  /**
   * Delete the focus and move to the last element in a JSON array.
   *
   * @group ArrayModification
   */
  def deleteGoLast: ACursor

  /**
   * Delete all values to the left of the focus in a JSON array.
   *
   * @group ArrayModification
   */
  def deleteLefts: ACursor

  /**
   * Delete all values to the right of the focus in a JSON array.
   *
   * @group ArrayModification
   */
  def deleteRights: ACursor

  /**
   * Replace all values to the left of the focus in a JSON array.
   *
   * @group ArrayModification
   */
  def setLefts(x: Vector[Json]): ACursor

  /**
   * Replace all values to the right of the focus in a JSON array.
   *
   * @group ArrayModification
   */
  def setRights(x: Vector[Json]): ACursor

  /**
   * Delete the focus and move to the sibling with the given key in a JSON object.
   *
   * @group ObjectModification
   */
  def deleteGoField(k: String): ACursor

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
      case Right(None) => Right(fallback)
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[A]]
    }

  /**
   * Replay an operation against this cursor.
   *
   * @group Utilities
   */
  final def replayOne(op: CursorOp): ACursor = op match {
    case CursorOp.MoveLeft => left
    case CursorOp.MoveRight => right
    case CursorOp.MoveFirst => first
    case CursorOp.MoveLast => last
    case CursorOp.MoveUp => up
    case CursorOp.LeftN(n) => leftN(n)
    case CursorOp.RightN(n) => rightN(n)
    case CursorOp.LeftAt(p) => leftAt(p)
    case CursorOp.RightAt(p) => rightAt(p)
    case CursorOp.Find(p) => find(p)
    case CursorOp.Field(k) => field(k)
    case CursorOp.DownField(k) => downField(k)
    case CursorOp.DownArray => downArray
    case CursorOp.DownAt(p) => downAt(p)
    case CursorOp.DownN(n) => downN(n)
    case CursorOp.DeleteGoParent => delete
    case CursorOp.DeleteGoLeft => deleteGoLeft
    case CursorOp.DeleteGoRight => deleteGoRight
    case CursorOp.DeleteGoFirst => deleteGoFirst
    case CursorOp.DeleteGoLast => deleteGoLast
    case CursorOp.DeleteGoField(k) => deleteGoField(k)
    case CursorOp.DeleteLefts => deleteLefts
    case CursorOp.DeleteRights => deleteRights
    case CursorOp.SetLefts(js) => setLefts(js)
    case CursorOp.SetRights(js) => setRights(js)
  }

  /**
   * Replay history (a list of operations in reverse "chronological" order) against this cursor.
   *
   * @group Utilities
   */
  final def replay(history: List[CursorOp]): ACursor = history.foldRight(this)((op, c) => c.replayOne(op))
}

final object ACursor {
  private[this] val jsonOptionEq: Eq[Option[Json]] = cats.kernel.instances.option.catsKernelStdEqForOption(Json.eqJson)

  implicit val eqACursor: Eq[ACursor] = Eq.instance((a, b) =>
    jsonOptionEq.eqv(a.focus, b.focus) && CursorOp.eqCursorOpList.eqv(a.history, b.history)
  )
}
