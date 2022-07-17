package io.circe

import cats._
import scala.annotation.tailrec
import cats.syntax.all._

/**
 * A data type that represents a position in a JSON document and supports
 * navigation and modification. This type of structure is commonly referred to
 * as a "Zipper".
 *
 * The `focus` represents the current position of the cursor; it may be
 * updated with `mapFocus` or changed using navigation methods like `left` and
 * `right`.
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
 * @see [[https://en.wikipedia.org/wiki/Zipper_(data_structure) Zipper]]
 */
sealed abstract class Cursor extends Serializable {
  import Cursor._

  /**
   * The last cursor state. In the first state this will be `null`.
   *
   * @note `null` is used to avoid boxing as this value is used for
   *       performance sensitive operations. Users of the API should (and
   *       must) use [[#lastCursor]] which is safe.
   */
  protected def unsafeLastCursor: Cursor.SuccessCursor

  /**
   * The last cursor op. In the first state this will be `null`.
   *
   * @note `null` is used to avoid boxing as this value is used for
   *       performance sensitive operations. Users of the API should (and
   *       must) use [[#lastOp]] which is safe.
   */
  protected def unsafeLastOp: CursorOp

  /**
   * The current location in the document. It is empty in failure states.
   *
   * @group Access
   */
  def focus: Option[Json]

  /**
   * Move the focus to the parent.
   *
   * @group Access
   */
  def up: Cursor

  /**
   * Delete the focus and move to its parent.
   *
   * @group Modification
   */
  def delete: Cursor

  /**
   * Modify the focus using the given function.
   *
   * @group Modification
   */
  def mapFocus(f: Json => Json): Cursor

  /**
   * Modify the focus in a context using the given function.
   *
   * @group Modification
   */
  def mapFocusA[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[Cursor]

  /**
   * If the focus is an element in a JSON array, move to the left.
   *
   * @group ArrayNavigation
   */
  def left: Cursor

  /**
   * If the focus is an element in a JSON array, move to the right.
   *
   * @group ArrayNavigation
   */
  def right: Cursor

  /**
   * If the focus is a value in a JSON array, return the index.
   *
   * @group ArrayAccess
   */
  def index: Option[Int]

  /**
   * If the focus is a value in a JSON object, return the key.
   *
   * @group ObjectAccess
   */
  def key: Option[String]

  /**
   * If the focus is a JSON array, move to its first element.
   *
   * @group ArrayNavigation
   */
  def downArray: Cursor

  /**
   * If the focus is a JSON array, move to the element at the given index.
   *
   * @group ArrayNavigation
   */
  def downN(n: Int): Cursor

  /**
   * If the focus is a value in a JSON object, move to a sibling with the given key.
   *
   * @group ObjectNavigation
   */
  def field(k: String): Cursor

  /**
   * If the focus is a JSON object, move to the value of the given key.
   *
   * @group ObjectNavigation
   */
  def downField(k: String): Cursor

  /**
   * Whether or not this cursor is pointed to the root, e.g. top, of the JSON
   * structure.
   *
   * @note While the first state of a cursor will always be the root, it is
   *       important to recall that other states may also be the root. For
   *       example, `_.downArray.up` will point to the root as well (assuming
   *       a successful traversal).
   */
  def isRoot: Boolean

  /**
   * If the cursor is in a failed state, this is a description of that failed
   * state.
   */
  def failureReason: Option[CursorFailureReason]

  // final //

  /**
   * The last, e.g. previous, cursor state. The last cursor, if present, must
   * always be a success cursor. This is because the only non-success cursor
   * is a failed cursor, which is a terminal state. Being a terminal state, it
   * can't be left, so it can't be a previous state.
   *
   * The last state will be present in all cursor states ''except'' for the
   * very first state.
   */
  final def lastCursor: Option[Cursor.SuccessCursor] =
    Option(unsafeLastCursor)

  /**
   * The last, e.g. previous, cursor operation. Similar to the [[#lastCursor]],
   * the last op must be present in all states except the first.
   */
  final def lastOp: Option[CursorOp] =
    Option(unsafeLastOp)

  /** The history of all the operations this cursor has done. */
  final lazy val history: Cursor.History = {

    @tailrec
    def loop(acc: List[CursorOp], cursor: Cursor): History =
      if (cursor.unsafeLastOp eq null) {
        History.fromList(acc.reverse)
      } else {
        loop(cursor.unsafeLastOp +: acc, cursor.unsafeLastCursor)
      }

    loop(List.empty, this)
  }

  /**
   * Whether or not this cursor is successful.
   *
   * A successful cursor will always have a non-empty focus.
   */
  final def succeeded: Boolean =
    focus.nonEmpty

  /**
   * Whether or not this cursor has failed.
   *
   * A failed cursor will always have an empty focus.
   */
  final def failed: Boolean = succeeded === false

  /**
   * Move from the current position to the root of the JSON structure.
   *
   * If the cursor is a [[Cursor#SuccessCursor]], this is the same as calling
   * [[#up]] until [[#isRoot]] returns true. If the cursor is a has failed,
   * then this will not move the cursor. The only way to exit a cursor in a
   * failed state is via the [[#lastCursor]].
   */
  @tailrec
  final def root: Cursor =
    this match {
      case cursor if cursor.isRoot =>
        this
      case _: FailedCursor =>
        this
      case otherwise =>
        otherwise.up.root
    }

  /**
   * Replace the focus.
   *
   * @group Modification
   */
  final def set(j: Json): Cursor = mapFocus(_ => j)

  /**
   * If the focus is a JSON array, return its elements.
   *
   * @group ArrayAccess
   */
  final def values: Option[Iterable[Json]] =
    focus.flatMap {
      case Json.JArray(vs) => Some(vs)
      case _               => None
    }

  /**
   * If the focus is a JSON object, return its field names in their original order.
   *
   * @group ObjectAccess
   */
  final def keys: Option[Iterable[String]] =
    focus.flatMap {
      case Json.JObject(o) => Some(o.keys)
      case _               => None
    }

  /**
   * Return to the root of the document.
   *
   * @group Access
   */
  final def top: Option[Json] =
    root.focus

  /**
   * Replay an operation against this cursor.
   *
   * @group Utilities
   */
  final def replayOne(op: CursorOp): Cursor = op match {
    case CursorOp.MoveLeft       => left
    case CursorOp.MoveRight      => right
    case CursorOp.MoveUp         => up
    case CursorOp.Field(k)       => field(k)
    case CursorOp.DownField(k)   => downField(k)
    case CursorOp.DownArray      => downArray
    case CursorOp.DownN(n)       => downN(n)
    case CursorOp.DeleteGoParent => delete
    case CursorOp.Replace(json)  => set(json)
  }

  /**
   * Replay history against this cursor.
   *
   * @group Utilities
   */
  final def replay(history: History): Cursor =
    if (failed) {
      // No need to waste anyone's time.
      this
    } else {
      history.asList.reverse.foldLeft(this) {
        case (acc, value) =>
          acc.replayOne(value)
      }
    }
}

object Cursor {

  // Developer's note
  //
  // Many of cursor methods have very similar implementations for all
  // SuccessCursor values, i.. TopCursor, ArrayCursor, ObjectCursor. For this
  // reason, it is the current (2022-07-19) desgin idiom to define these as
  // final in SuccessCursor so that the common parts may be
  // shared. FailedCursor implementations are usually trivial and different,
  // so they are defined in FailedCursor.
  //
  // Historically it was not possible to do this because the old
  // io.circe.ACursor was not a sealed type, meaning we couldn't enumerate all
  // the cases safely. However io.circe.Cursor is sealed so these are no
  // longer modeling problems.
  //
  // Most types in this companion object should be private, with the exception
  // of SuccessCursor (and honestly I even debated if this should be
  // public). The reason being is that it is likely we will need to evolve
  // this in the future and we want to keep the implementation details hidden
  // to minimize binary compatiblity (bincompat) issues.

  /**
   * A [[Cursor]] which is successful.
   *
   * [[SuccessCursor]] values always have a non-empty focus which can be
   * accessed via [[#value]].
   */
  sealed abstract class SuccessCursor extends Cursor {

    /**
     * The focus of the current cursor, which is always present on
     * [[SuccessCursor]] values.
     */
    def value: Json

    override final def focus: Some[Json] = Some(value)

    override final def up: Cursor = {
      val op: CursorOp = CursorOp.MoveUp

      this match {
        case _: TopCursor =>
          fail(CursorOp.MoveUp, CursorFailureReason.MoveUpFromTop)
        case cursor: NormalCursor if cursor.changed =>
          val parentValue: Json =
            cursor match {
              case cursor: ArrayCursor =>
                Json.fromValues(cursor.arrayValues)
              case cursor: ObjectCursor =>
                Json.fromJsonObject(cursor.obj)
            }
          cursor.parent match {
            case parent: ArrayCursor =>
              ArrayCursor(
                parent.arrayValues.updated(parent.indexValue, parentValue),
                parent.indexValue,
                this,
                op,
                parent.parent,
                true
              )
            case parent: ObjectCursor =>
              ObjectCursor(
                parent.obj.add(parent.keyValue, parentValue),
                parent.keyValue,
                this,
                op,
                parent.parent,
                true
              )
            case _: TopCursor =>
              TopCursor(parentValue, this, op)
          }
        case cursor: NormalCursor =>
          // Not changed, so we don't have to mess with the parent's focus.
          cursor.parent match {
            case parent: ArrayCursor =>
              ArrayCursor(
                parent.arrayValues,
                parent.indexValue,
                this,
                op,
                parent.parent,
                true
              )
            case parent: ObjectCursor =>
              ObjectCursor(
                parent.obj,
                parent.keyValue,
                this,
                op,
                parent.parent,
                true
              )
            case parent: TopCursor =>
              TopCursor(parent.value, this, op)
          }
      }
    }

    override final def delete: Cursor = {
      val op: CursorOp = CursorOp.DeleteGoParent

      this match {
        case _: TopCursor =>
          fail(op, CursorFailureReason.MoveUpFromTop)
        case cursor: NormalCursor =>
          // The focus of the parent with the current focus deleted. We don't need
          // to reference parent here because in ArrayCursor arrayValues is the
          // parent's focus and in ObjectCursor obj is the parent's focus.
          val except: Json =
            cursor match {
              case cursor: ArrayCursor =>
                val arrayValues: Vector[Json] = cursor.arrayValues
                val indexValue: Int = cursor.indexValue
                Json.fromValues(arrayValues.take(indexValue) ++ arrayValues.drop(indexValue + 1))
              case cursor: ObjectCursor =>
                Json.fromJsonObject(cursor.obj.remove(cursor.keyValue))
            }

          cursor.parent match {
            case parent: ArrayCursor =>
              ArrayCursor(
                parent.arrayValues.updated(parent.indexValue, except),
                parent.indexValue,
                this,
                op,
                parent.parent,
                true
              )
            case parent: ObjectCursor =>
              ObjectCursor(
                parent.obj.add(parent.keyValue, except),
                parent.keyValue,
                this,
                op,
                parent.parent,
                true
              )
            case _: TopCursor =>
              TopCursor(
                except,
                this,
                op
              )
          }
      }
    }

    override final def mapFocus(f: Json => Json): Cursor = {
      val newValue: Json = f(value)
      val op: CursorOp = CursorOp.Replace(newValue)

      this match {
        case cursor: ArrayCursor =>
          val indexValue: Int = cursor.indexValue
          ArrayCursor(cursor.arrayValues.updated(indexValue, newValue), indexValue, this, op, cursor.parent, true)
        case cursor: ObjectCursor =>
          val keyValue: String = cursor.keyValue
          ObjectCursor(cursor.obj.add(keyValue, newValue), keyValue, this, op, cursor.parent, true)
        case _: TopCursor =>
          TopCursor(newValue, this, op)
      }
    }

    override final def mapFocusA[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[Cursor] =
      f(value).map(value => mapFocus(_ => value))

    override final def left: Cursor =
      this match {
        case cursor: ArrayCursor =>
          if (cursor.indexValue > 0) {
            ArrayCursor(
              cursor.arrayValues,
              cursor.indexValue - 1,
              this,
              CursorOp.MoveLeft,
              cursor.parent,
              cursor.changed
            )
          } else {
            fail(
              CursorOp.MoveLeft,
              CursorFailureReason.MoveLeftIndexOutOfBoundsFailure
            )
          }
        case _ =>
          fail(
            CursorOp.MoveLeft,
            CursorFailureReason.ExpectedFocusToBeMemberOfJsonArray
          )
      }

    override final def right: Cursor =
      this match {
        case cursor: ArrayCursor =>
          if (cursor.indexValue < Int.MaxValue && cursor.arrayValues.size > cursor.indexValue + 1) {
            ArrayCursor(
              cursor.arrayValues,
              cursor.indexValue + 1,
              this,
              CursorOp.MoveRight,
              cursor.parent,
              cursor.changed
            )
          } else {
            fail(
              CursorOp.MoveRight,
              CursorFailureReason.MoveRightIndexOutOfBoundsFailure(cursor.indexValue.toLong + 1L)
            )
          }
        case _ =>
          fail(
            CursorOp.MoveRight,
            CursorFailureReason.ExpectedFocusToBeMemberOfJsonArray
          )
      }

    override final def index: Option[Int] =
      this match {
        case cursor: ArrayCursor =>
          Some(cursor.indexValue)
        case _ => None
      }

    override final def key: Option[String] =
      this match {
        case cursor: ObjectCursor =>
          Some(cursor.keyValue)
        case _ =>
          None
      }

    override final def downArray: Cursor = {
      val op: CursorOp = CursorOp.DownArray
      value match {
        case Json.JArray(values) =>
          if (values.isEmpty) {
            fail(op, CursorFailureReason.DownArrayOutOfBounds(n = 0, actualSize = 0))
          } else {
            ArrayCursor(
              values,
              0,
              this,
              op,
              this,
              false
            )
          }
        case otherwise =>
          fail(
            op,
            CursorFailureReason.IncorrectFocusTypeForNavigationOperation(
              expected = JsonTypeDescription.JsonArray,
              actual = JsonTypeDescription.fromJson(otherwise)
            )
          )
      }
    }

    override final def downN(n: Int): Cursor = {
      val op: CursorOp = CursorOp.DownN(n)
      value match {
        case Json.JArray(values) =>
          if (n < values.size) {
            ArrayCursor(
              values,
              n,
              this,
              op,
              this,
              false
            )
          } else {
            fail(op, CursorFailureReason.DownArrayOutOfBounds(n = n, actualSize = values.size))
          }
        case otherwise =>
          fail(
            op,
            CursorFailureReason.IncorrectFocusTypeForNavigationOperation(
              expected = JsonTypeDescription.JsonArray,
              actual = JsonTypeDescription.fromJson(otherwise)
            )
          )
      }
    }

    override final def field(k: String): Cursor = {
      val op: CursorOp = CursorOp.Field(k)

      this match {
        case cursor: ObjectCursor =>
          if (cursor.obj.contains(k) === false) {
            fail(op, CursorFailureReason.NoSiblingWithMatchingKeyName(k))
          } else {
            ObjectCursor(cursor.obj, k, this, op, cursor.parent, cursor.changed)
          }
        case _: ArrayCursor =>
          fail(op, CursorFailureReason.NoSiblingsInArrayContext(k))
        case _: TopCursor =>
          fail(op, CursorFailureReason.NoSiblingsInTopContext(k))
      }
    }

    override final def downField(k: String): Cursor = {
      val op: CursorOp = CursorOp.DownField(k)

      value match {
        case Json.JObject(obj) =>
          if (obj.contains(k)) {
            ObjectCursor(obj, k, this, op, this, false)
          } else {
            fail(op, CursorFailureReason.MissingKeyInObject(k))
          }
        case otherwise =>
          fail(
            op,
            CursorFailureReason.IncorrectFocusTypeForNavigationOperation(
              expected = JsonTypeDescription.JsonObject,
              actual = JsonTypeDescription.fromJson(otherwise)
            )
          )
      }
    }

    override final def failureReason: Option[CursorFailureReason] = None

    private final def fail(op: CursorOp, reason: CursorFailureReason): FailedCursor =
      FailedCursor(this, op, reason)
  }

  /** A cursor pointing to the top, i.e. root, of a JSON structure. */
  private[this] sealed abstract class TopCursor extends SuccessCursor {
    override final def isRoot: Boolean = true

    override final def toString: String =
      s"TopCursor(value = $value)"
  }

  private[this] object TopCursor {
    private[this] final case class TopCursorImpl(
      override val value: Json,
      override protected val unsafeLastCursor: Cursor.SuccessCursor,
      override protected val unsafeLastOp: CursorOp
    ) extends TopCursor

    private[Cursor] def apply(value: Json): TopCursor =
      TopCursor(value, null, null)

    private[Cursor] def apply(value: Json, unsafeLastCursor: Cursor.SuccessCursor, unsafeLastOp: CursorOp): TopCursor =
      TopCursorImpl(value, unsafeLastCursor, unsafeLastOp)
  }

  /**
   * An internal trait for "normal" cursors. "normal" here means not the top
   * and not a failure state. This is useful because "normal" cursors have
   * some common properties which it is occassionaly nice to match on.
   */
  private[this] sealed abstract class NormalCursor extends SuccessCursor {
    def parent: SuccessCursor
    def changed: Boolean

    override final def isRoot: Boolean = false
  }

  /** A cursor which is pointing at an element of a JSON array. */
  private[this] sealed abstract class ArrayCursor extends NormalCursor {
    def arrayValues: Vector[Json]
    def indexValue: Int

    override final def value: Json =
      arrayValues(indexValue)

    override final def toString: String =
      s"ArrayCursor(value = $value, indexValue = $indexValue, arrayValues = $arrayValues)"
  }

  private[this] object ArrayCursor {
    private[this] final case class ArrayCursorImpl(
      override val arrayValues: Vector[Json],
      override val indexValue: Int,
      override protected val unsafeLastCursor: Cursor.SuccessCursor,
      override protected val unsafeLastOp: CursorOp,
      override val parent: SuccessCursor,
      override val changed: Boolean
    ) extends ArrayCursor

    private[Cursor] def apply(
      arrayValues: Vector[Json],
      indexValue: Int,
      lastCursor: Cursor.SuccessCursor,
      lastOp: CursorOp,
      parent: SuccessCursor,
      changed: Boolean
    ): ArrayCursor =
      ArrayCursorImpl(arrayValues, indexValue, lastCursor, lastOp, parent, changed)
  }

  /** A cursor which is pointing at an key, i.e. field, in a JSON object. */
  private[this] sealed abstract class ObjectCursor extends NormalCursor {
    def obj: JsonObject
    def keyValue: String

    override final def value: Json =
      obj.applyUnsafe(keyValue)

    override final def toString: String =
      s"ObjectCursor(value = $value, obj = $obj, keyValue = $keyValue)"
  }

  private[this] object ObjectCursor {
    private[this] final case class ObjectCursorImpl(
      override val obj: JsonObject,
      override val keyValue: String,
      override protected val unsafeLastCursor: Cursor.SuccessCursor,
      override protected val unsafeLastOp: CursorOp,
      override val parent: SuccessCursor,
      override val changed: Boolean
    ) extends ObjectCursor

    private[Cursor] def apply(
      obj: JsonObject,
      keyValue: String,
      unsafeLastCursor: Cursor.SuccessCursor,
      unsafeLastOp: CursorOp,
      parent: SuccessCursor,
      changed: Boolean
    ): ObjectCursor =
      ObjectCursorImpl(obj, keyValue, unsafeLastCursor, unsafeLastOp, parent, changed)
  }

  /**
   * A cursor in a failed state. The only way to exit this state is via the
   * [[Cursor#lastCursor]] method.
   */
  private sealed abstract class FailedCursor extends Cursor {
    def failureReasonValue: CursorFailureReason

    override final def failureReason: Some[CursorFailureReason] = Some(failureReasonValue)
    override final def focus: Option[Json] = None
    override final def up: Cursor = this
    override final def delete: Cursor = this
    override final def index: Option[Int] = None
    override final def key: Option[String] = None
    override final def left: Cursor = this
    override final def right: Cursor = this
    override final def mapFocus(f: Json => Json): Cursor = this
    override final def mapFocusA[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[Cursor] = F.pure(this)
    override final def downArray: Cursor = this
    override final def downN(n: Int): Cursor = this
    override final def field(k: String): Cursor = this
    override final def downField(k: String): Cursor = this
    override final def isRoot: Boolean = false

    override final def toString: String =
      s"FailedCursor(failureReason = $failureReason)"
  }

  private object FailedCursor {
    private[this] final case class FailedCursorImpl(
      override protected val unsafeLastCursor: Cursor.SuccessCursor,
      override protected val unsafeLastOp: CursorOp,
      override val failureReasonValue: CursorFailureReason
    ) extends FailedCursor

    private[Cursor] def apply(
      unsafeLastCursor: Cursor.SuccessCursor,
      unsafeLastOp: CursorOp,
      failureReason: CursorFailureReason
    ): FailedCursor =
      FailedCursorImpl(unsafeLastCursor, unsafeLastOp, failureReason)
  }

  /** A representation of the cursor history. */
  sealed abstract class History extends Product with Serializable {
    def asList: List[CursorOp]

    override final def toString: String =
      s"""History(${asList.mkString(", ")})"""
  }

  object History {
    private[this] final case class HistoryImpl(override val asList: List[CursorOp]) extends History

    def fromList(value: List[CursorOp]): History =
      HistoryImpl(value)
  }

  /**
   * In the event of a failed cursor traversal, this will describe why the
   * failure occurred.
   */
  sealed abstract class CursorFailureReason extends RuntimeException {
    override final def fillInStackTrace(): Throwable = this
  }

  object CursorFailureReason {
    private[Cursor] final case object MoveLeftIndexOutOfBoundsFailure extends CursorFailureReason {
      override def getMessage: String = "Attempted to move to index value -1 in JSON array."
    }

    private[Cursor] final case class MoveRightIndexOutOfBoundsFailure(index: Long) extends CursorFailureReason {
      override def getMessage: String = s"Attempted to move to out of bounds array index ${index + 1L}."
    }

    // TODO: Record the index we were trying to navigate to? Does that always apply?
    private[Cursor] final case object ExpectedFocusToBeMemberOfJsonArray extends CursorFailureReason {
      override def getMessage: String = "Attempted to move in JSON array, but focus was not a member of a JSON array."
    }

    private[Cursor] case object MoveUpFromTop extends CursorFailureReason {
      override def getMessage: String = "Attempted to move up from TopCursor. There is no up from TopCursor."
    }

    // TODO: Do we want to record the type of navigation operation here? Some
    // navigation ops can fail due to other reasons and some can't ever fail
    // due to this reason (MoveUp).
    private[Cursor] final case class IncorrectFocusTypeForNavigationOperation(
      expected: JsonTypeDescription,
      actual: JsonTypeDescription
    ) extends CursorFailureReason {
      override def getMessage: String =
        s"Unable to navigate at focus. Expected JSON of type ${expected.value}, but got ${actual.value}."
    }

    private[Cursor] final case class DownArrayOutOfBounds(n: Int, actualSize: Int) extends CursorFailureReason {
      override def getMessage: String =
        s"Attempted to enter into JSON array at index ${n}, but that is out of bounds for the array of size ${actualSize}."
    }

    private[Cursor] final case class NoSiblingWithMatchingKeyName(targetFocusKey: String) extends CursorFailureReason {
      override def getMessage: String =
        s"""Attempted to navigate to sibling in JSON object with key name "$targetFocusKey", but no such key exists."""
    }

    private[Cursor] final case class NoSiblingsInTopContext(targetFocusKey: String) extends CursorFailureReason {
      override def getMessage: String =
        s"""Attempted to navigate to sibling in JSON object with key name "${targetFocusKey}", but focus was the Root of the JSON structure, not an object member."""
    }

    private[Cursor] final case class NoSiblingsInArrayContext(targetFocusKey: String) extends CursorFailureReason {
      override def getMessage: String =
        s"""Attempted to navigate to sibling in JSON object with key name "${targetFocusKey}", but focus was a member of a JSON array, not an object."""
    }

    private[Cursor] final case class MissingKeyInObject(targetFocusKey: String) extends CursorFailureReason {
      override def getMessage: String =
        s"""Attempted to navigate to key ${targetFocusKey} in JSON object, but JSON object does not have such a key."""
    }
  }

  /** An ADT to denote the type of some JSON value, used mostly in errors. */
  private[this] sealed abstract class JsonTypeDescription extends Serializable {
    import JsonTypeDescription._

    final def value: String =
      this match {
        case JsonNull    => "null"
        case JsonBoolean => "boolean"
        case JsonNumber  => "number"
        case JsonString  => "string"
        case JsonArray   => "array"
        case JsonObject  => "object"
      }
  }

  private[this] object JsonTypeDescription {
    case object JsonNull extends JsonTypeDescription
    case object JsonBoolean extends JsonTypeDescription
    case object JsonNumber extends JsonTypeDescription
    case object JsonString extends JsonTypeDescription
    case object JsonArray extends JsonTypeDescription
    case object JsonObject extends JsonTypeDescription

    def fromJson(value: Json): JsonTypeDescription = {
      import Json._

      value match {
        case JNull       => JsonNull
        case JBoolean(_) => JsonBoolean
        case JNumber(_)  => JsonNumber
        case JString(_)  => JsonString
        case JArray(_)   => JsonArray
        case JObject(_)  => JsonObject
      }
    }
  }

  /**
   * Create a [[Cursor]] from a [[Json]] value.
   *
   * The initial cursor created will always be a [[Cursor#SuccessCursor]].
   */
  def fromJson(value: Json): Cursor.SuccessCursor =
    TopCursor(value)
}
