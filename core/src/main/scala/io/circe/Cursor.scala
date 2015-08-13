package io.circe

import io.circe.cursor.{ CJson, CursorOperations }

/**
 * A zipper that represents a position in a JSON value and supports navigation around the JSON
 * value.
 *
 * The `focus` represents the current position of the cursor; it may be updated with `withFocus` or
 * changed using the navigation methods `left`, `right`, etc.
 *
 * @groupname Ungrouped Cursor fields and operations
 * @groupprio Ungrouped 1
 *
 * @see [[GenericCursor]]
 * @author Travis Brown
 */
abstract class Cursor extends CursorOperations {
  /**
   * Return the current context of the focus.
   */
  def context: List[Context]

  /**
   * Create an [[HCursor]] for this cursor in order to track history.
   */
  def hcursor: HCursor = HCursor(this, Nil)
}

object Cursor {
  /**
   * Create a new cursor with no context.
   */
  def apply(j: Json): Cursor = CJson(j)
}
