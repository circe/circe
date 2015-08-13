package io.circe

import cats.data.Xor

/**
 * A zipper that represents a position in a JSON document and supports navigation and modification.
 *
 * The `focus` represents the current position of the cursor; it may be updated with `withFocus` or
 * changed using navigation methods like `left` and `right`.
 *
 * circe includes three kinds of cursors. [[Cursor]] is the simplest: it doesn't keep track of its
 * history. [[HCursor]] is a cursor that does keep track of its history, but does not represent the
 * possibility that a navigation or modification operation has failed. [[ACursor]] is the richest
 * cursor, since it both tracks history through an underlying [[HCursor]] and can represent failed
 * operations.
 *
 * [[GenericCursor]] is an abstraction over these three types, and it has several abstract type
 * members that are required in order to represent the different roles of the three cursor types.
 * `Self` is simply the specific type of the cursor, `Focus` is a type constructor that represents
 * the context in which the focus is available, `Result` is the type that is returned by all
 * navigation and modification operations, and `M` is a type class that includes the operations that
 * we need for `withFocusM`.
 *
 * @groupname TypeMembers Type members
 * @groupprio TypeMembers 0
 * @groupname Access Access and navigation
 * @groupprio Access 2
 * @groupname Modification Modification
 * @groupprio Modification 3
 * @groupname ArrayAccess Array access
 * @groupprio ArrayAccess 4
 * @groupname ObjectAccess Object access
 * @groupprio ObjectAccess 5
 * @groupname ArrayNavigation Array navigation
 * @groupprio ArrayNavigation 6
 * @groupname ObjectNavigation Object navigation
 * @groupprio ObjectNavigation 7
 * @groupname ArrayModification Array modification
 * @groupprio ArrayModification 8
 * @groupname ObjectModification Object modification
 * @groupprio ObjectModification 9
 * @groupname Decoding Decoding
 * @groupprio Decoding 10
 *
 * @author Travis Brown
 */
trait GenericCursor[C <: GenericCursor[C]] {
  /**
   * The context that the cursor is available in.
   *
   * @group TypeMembers
   */
  type Focus[_]

  /**
   * The type returned by navigation and modifications operations.
   *
   * @group TypeMembers
   */
  type Result

  /**
   * The current location in the document.
   *
   * @group Access
   */
  def focus: Focus[Json]

  /**
   * Return to the root of the document.
   *
   * @group Access
   */
  def top: Focus[Json]

  /**
   * Move the focus to the parent.
   *
   * @group Access
   */
  def up: Result

  /**
   * Delete the focus and move to its parent.
   *
   * @group Modification
   */
  def delete: Result

  /**
   * Modify the focus using the given function.
   *
   * @group Modification
   */
  def withFocus(f: Json => Json): C

  /**
   * Replace the focus.
   *
   * @group Modification
   */
  def set(j: Json): C = withFocus(_ => j)

  /**
   * If the focus is a JSON array, return the elements to the left.
   *
   * @group ArrayAccess
   */
  def lefts: Option[List[Json]]

  /**
   * If the focus is a JSON array, return the elements to the right.
   *
   * @group ArrayAccess
   */
  def rights: Option[List[Json]]

  /**
   * If the focus is a JSON object, return its field names in a set.
   *
   * @group ObjectAccess
   */
  def fieldSet: Option[Set[String]]

  /**
   * If the focus is a JSON object, return its field names in their original order.
   *
   * @group ObjectAccess
   */
  def fields: Option[List[String]]

  /**
   * If the focus is an element in a JSON array, move to the left.
   *
   * @group ArrayNavigation
   */
  def left: Result

  /**
   * If the focus is an element in a JSON array, move to the right.
   *
   * @group ArrayNavigation
   */
  def right: Result

  /**
   * If the focus is an element in a JSON array, move to the first element.
   *
   * @group ArrayNavigation
   */
  def first: Result

  /**
   * If the focus is an element in a JSON array, move to the last element.
   *
   * @group ArrayNavigation
   */
  def last: Result

  /**
   * If the focus is an element in JSON array, move to the left the given number of times.
   *
   * A negative value will move the cursor right.
   *
   * @group ArrayNavigation
   */
  def leftN(n: Int): Result

  /**
   * If the focus is an element in JSON array, move to the right the given number of times.
   *
   * A negative value will move the cursor left.
   *
   * @group ArrayNavigation
   */
  def rightN(n: Int): Result

  /**
   * If the focus is an element in a JSON array, move to the left until the given predicate matches
   * the new focus.
   *
   * @group ArrayNavigation
   */
  def leftAt(p: Json => Boolean): Result

  /**
   * If the focus is an element in a JSON array, move to the right until the given predicate matches
   * the new focus.
   *
   * @group ArrayNavigation
   */
  def rightAt(p: Json => Boolean): Result

  /**
   * If the focus is an element in a JSON array, find the first element at or to its right that
   * matches the given predicate.
   *
   * @group ArrayNavigation
   */
  def find(p: Json => Boolean): Result

  /**
   * If the focus is a JSON array, move to its first element.
   *
   * @group ArrayNavigation
   */
  def downArray: Result

  /**
   * If the focus is a JSON array, move to the first element that satisfies the given predicate.
   *
   * @group ArrayNavigation
   */
  def downAt(p: Json => Boolean): Result

  /**
   * If the focus is a JSON array, move to the element at the given index.
   *
   * @group ArrayNavigation
   */
  def downN(n: Int): Result

  /**
   * If the focus is a value in a JSON object, move to a sibling with the given key.
   *
   * @group ObjectNavigation
   */
  def field(k: String): Result

  /**
   * If the focus is a JSON object, move to the value of the given key.
   *
   * @group ObjectNavigation
   */
  def downField(k: String): Result

  /**
   * Delete the focus and move to the left in a JSON array.
   *
   * @group ArrayModification
   */
  def deleteGoLeft: Result

  /**
   * Delete the focus and move to the right in a JSON array.
   *
   * @group ArrayModification
   */
  def deleteGoRight: Result

  /**
   * Delete the focus and move to the first element in a JSON array.
   *
   * @group ArrayModification
   */
  def deleteGoFirst: Result

  /**
   * Delete the focus and move to the last element in a JSON array.
   *
   * @group ArrayModification
   */
  def deleteGoLast: Result

  /**
   * Delete all values to the left of the focus in a JSON array.
   *
   * @group ArrayModification
   */
  def deleteLefts: Result

  /**
   * Delete all values to the right of the focus in a JSON array.
   *
   * @group ArrayModification
   */
  def deleteRights: Result

  /**
   * Replace all values to the left of the focus in a JSON array.
   *
   * @group ArrayModification
   */
  def setLefts(x: List[Json]): Result

  /**
   * Replace all values to the right of the focus in a JSON array.
   *
   * @group ArrayModification
   */
  def setRights(x: List[Json]): Result

  /**
   * Delete the focus and move to the sibling with the given key in a JSON object.
   *
   * @group ObjectModification
   */
  def deleteGoField(k: String): Result

  /**
   * Attempt to decode the focus as an `A`.
   *
   * @group Decoding
   */
  def as[A](implicit d: Decoder[A]): Xor[DecodingFailure, A]

  /**
   * Attempt to decode the value at the given key in a JSON object as an `A`.
   *
   * @group Decoding
   */
  def get[A](k: String)(implicit d: Decoder[A]): Xor[DecodingFailure, A]
}
