package io.jfc

import cats.data.Xor

/**
 * A zipper that represents a position in a JSON document and supports
 * navigation and modification.
 * 
 * The `focus` represents the current position of the cursor; it may be updated
 * with `withFocus` or changed using navigation methods like `left` and `right`.
 *
 * jfc includes three kinds of cursors. [[Cursor]] is the simplest: it doesn't
 * keep track of its history. [[HCursor]] is a cursor that does keep track of
 * its history, but does not represent the possibility that an navigation or
 * modification operation has failed. [[ACursor]] is the richest cursor, since 
 * it both tracks history through an underlying [[HCursor]] and can represent
 * failed operations.
 *
 * [[GenericCursor]] is an abstraction over these three types, and it has
 * several abstract type members that are required in order to represent the
 * different roles of the three cursor types. `Self` is simply the specific type
 * of the cursor, `Focus` is a type constructor that represents the context in
 * which the focus is available, `Result` is the type that is returned by all
 * navigation and modification operations, and `M` is a type class that includes
 * the operations that we need for `withFocusM`.
 *
 * @author Travis Brown
 */
trait GenericCursor {
  type Self
  type Focus[_]
  type Result
  type M[_[_]]

  /**
   * The current location in the document.
   */
  def focus: Focus[Json]

  /**
   * Return to the root of the document.
   */
  def undo: Focus[Json]

  /**
   * Attempt to decode the focus as an `A`.
   */
  def as[A](implicit decode: Decode[A]): Xor[DecodeFailure, A]

  /**
   * Attempt to decode the value at the given key in a JSON object as an `A`.
   */
  def get[A](k: String)(implicit decode: Decode[A]): Xor[DecodeFailure, A]

  /**
   * Modify the focus using the given function.
   */
  def withFocus(f: Json => Json): Self

  /**
   * Modify the focus in a context using the given function.
   */
  def withFocusM[F[_]: M](f: Json => F[Json]): F[Self]

  /**
   * Replace the focus.
   */
  def set(j: Json): Self = withFocus(_ => j)

  /**
   * If the focus is a JSON array, return the elements to the left.
   */
  def lefts: Option[List[Json]]

  /**
   * If the focus is a JSON array, return the elements to the right.
   */
  def rights: Option[List[Json]]

  /**
   * If the focus is a JSON object, return its field names in a set.
   */
  def fieldSet: Option[Set[String]]

  /**
   * If the focus is a JSON object, return its field names in their original
   * order.
   */
  def fields: Option[List[String]]

  /**
   * If the focus is an element in a JSON array, move to the left.
   */
  def left: Result

  /**
   * If the focus is an element in a JSON array, move to the right.
   */
  def right: Result

  /**
   * If the focus is an element in a JSON array, move to the first element.
   */
  def first: Result

  /**
   * If the focus is an element in a JSON array, move to the last element.
   */
  def last: Result

  /**
   * If the focus is an element in JSON array, move to the left the given number
   * of times. A negative value will move the cursor right.
   */
  def leftN(n: Int): Result

  /**
   * If the focus is an element in JSON array, move to the right the given
   * number of times. A negative value will move the cursor left.
   */
  def rightN(n: Int): Result

  /**
   * If the focus is an element in a JSON array, move to the left until the
   * given predicate matches the new focus.
   */
  def leftAt(p: Json => Boolean): Result

  /**
   * If the focus is an element in a JSON array, move to the right until the
   * given predicate matches the new focus.
   */
  def rightAt(p: Json => Boolean): Result

  /**
   * If the focus is an element in a JSON array, find the first element at or to
   * its right that matches the given predicate.
   */
  def find(p: Json => Boolean): Result

  /**
   * If the focus is a value in a JSON object, move to a sibling with the given
   * key.
   */
  def field(k: String): Result

  /**
   * If the focus is a JSON object, move to the value of the given key.
   */
  def downField(k: String): Result

  /**
   * If the focus is a JSON array, move to its first element.
   */
  def downArray: Result

  /**
   * If the focus is a JSON array, move to the first element that satisfies the
   * given predicate.
   */
  def downAt(p: Json => Boolean): Result

  /**
   * If the focus is a JSON array, move to the element at the given index.
   */
  def downN(n: Int): Result

  /**
   * Delete the focus and move to its parent.
   */
  def delete: Result

  /**
   * Delete the focus and move to the left in a JSON array.
   */
  def deleteGoLeft: Result

  /**
   * Delete the focus and move to the right in a JSON array.
   */
  def deleteGoRight: Result

  /**
   * Delete the focus and move to the first element in a JSON array.
   */
  def deleteGoFirst: Result

  /**
   * Delete the focus and move to the last element in a JSON array.
   */
  def deleteGoLast: Result

  /**
   * Delete the focus and move to the sibling with the given key in a JSON
   * object.
   */
  def deleteGoField(k: String): Result

  /**
   * Delete all values to the left of the focus in a JSON array.
   */
  def deleteLefts: Result

  /**
   * Delete all values to the right of the focus in a JSON array.
   */
  def deleteRights: Result

  /**
   * Replace all values to the left of the focus in a JSON array.
   */
  def setLefts(x: List[Json]): Result

  /**
   * Replace all values to the right of the focus in a JSON array.
   */
  def setRights(x: List[Json]): Result

  /** Move the focus to the parent. */
  def up: Result
}
