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
    replayCursorOps(history.asList)

  /**
   * Replay history against the cursor for any `Foldable` of [[CursorOp]].
   *
   * @note In general, you should prefer [[#replay]]. [[#replay]] uses
   *       [[Cursor#History]] which can not have an invalid construction,
   *       e.g. nonsensical cursor ops. [[Cursor#History]] values can only be
   *       constructed from a [[Cursor]] instance, thus this method is
   *       provided when you need to manually construct history.
   */
  final def replayCursorOps[F[_]: Foldable](history: F[CursorOp]): Cursor =
    if (failed) {
      // No need to waste anyone's time.
      this
    } else {
      // Reverse, for all foldables
      val asReversedList: List[CursorOp] =
        history.foldLeft(List.empty[CursorOp]) {
          case value =>
            value.foldLeft(List.empty[CursorOp]) {
              case (acc, value) =>
                value +: acc
            }
        }
      asReversedList.foldLeft(this) {
        case (acc, value) =>
          acc.replayOne(value)
      }
    }

  /**
   * Get the [[CursorPath]] for the current focus of the [[Cursor]]. This is a
   * representation from the root of the JSON to the current focus value.
   */
  final def cursorPath: CursorPath = {
    import Cursor._
    import CursorPath._

    @tailrec
    def loop(acc: List[PathElem], cursor: Cursor): List[PathElem] =
      cursor match {
        case _: TopCursor =>
          acc
        case cursor: ObjectCursor =>
          loop(PathElem.JsonObjectKey(cursor.keyValue) :: acc, cursor.parent)
        case cursor: ArrayCursor =>
          loop(PathElem.JsonArrayIndex(cursor.indexValue.toLong) :: acc, cursor.parent)
        case cursor: FailedCursor =>
          loop(acc, cursor.unsafeLastCursor)
      }

    CursorPath.fromList(loop(Nil, this))
  }

  /**
   * As [[#cursorPath]], but represented as a `String`.
   */
  final def cursorPathString: String =
    cursorPath.pathString

  /**
   * Attempt to decode the focus as an `A`.
   *
   * @group Decoding
   */
  final def as[A](implicit d: Decoder[A]): Decoder.Result[A] =
    // Temporary definition until Decoder and DecodingFailure are refactored
    d.tryDecode(ACursor.fromCursor(this))

  /**
   * Attempt to decode the value at the given key in a JSON object as an `A`.
   *
   * @group Decoding
   */
  final def get[A: Decoder](k: String): Decoder.Result[A] =
    downField(k).as[A]

  /**
   * Attempt to decode the value at the given key in a JSON object as an `A`.
   * If the field `k` is missing, then use the `fallback` instead.
   *
   * @group Decoding
   */
  final def getOrElse[A: Decoder](k: String)(fallback: => A): Decoder.Result[A] =
    get[Option[A]](k) match {
      case Right(Some(a)) => Right(a)
      case Right(_)       => Right(fallback)
      case l @ Left(_)    => l.asInstanceOf[Decoder.Result[A]]
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
            CursorFailureReason.ExpectedFocusToBeMemberOfJsonArray(
              CursorFailureReason.FailureTarget.MoveLeftInNonArray.value
            )
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
            CursorFailureReason.ExpectedFocusToBeMemberOfJsonArray(
              CursorFailureReason.FailureTarget.MoveRightInNonArray.value
            )
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
              actual = JsonTypeDescription.fromJson(otherwise),
              failureTarget = CursorFailureReason.FailureTarget.JsonArrayIndexTarget(0L)
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
              actual = JsonTypeDescription.fromJson(otherwise),
              failureTarget = CursorFailureReason.FailureTarget.JsonArrayIndexTarget(n.toLong)
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
              actual = JsonTypeDescription.fromJson(otherwise),
              failureTarget = CursorFailureReason.FailureTarget.JsonObjectKeyTarget(k)
            )
          )
      }
    }

    override final def failureReason: Option[CursorFailureReason] = None

    private final def fail(op: CursorOp, reason: CursorFailureReason): FailedCursor =
      FailedCursor(this, op, reason)
  }

  /** A cursor pointing to the top, i.e. root, of a JSON structure. */
  private sealed abstract class TopCursor extends SuccessCursor {
    override final def isRoot: Boolean = true

    override final def toString: String =
      s"TopCursor(value = $value)"
  }

  private object TopCursor {
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
  private sealed abstract class NormalCursor extends SuccessCursor {
    def parent: SuccessCursor
    def changed: Boolean

    override final def isRoot: Boolean = false
  }

  /** A cursor which is pointing at an element of a JSON array. */
  private sealed abstract class ArrayCursor extends NormalCursor {
    def arrayValues: Vector[Json]
    def indexValue: Int

    override final def value: Json =
      arrayValues(indexValue)

    override final def toString: String =
      s"ArrayCursor(value = $value, indexValue = $indexValue, arrayValues = $arrayValues)"
  }

  private object ArrayCursor {
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
  private sealed abstract class ObjectCursor extends NormalCursor {
    def obj: JsonObject
    def keyValue: String

    override final def value: Json =
      obj.applyUnsafe(keyValue)

    override final def toString: String =
      s"ObjectCursor(value = $value, obj = $obj, keyValue = $keyValue)"
  }

  private object ObjectCursor {
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

    /**
     * FailedCursor values always have a failure reason.
     */
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

  /**
   * A representation of the cursor history. History instances can only be
   * constructed from [[Cursor]] values. This is to ensure they always
   * represent valid cursor historys.
   */
  sealed abstract class History extends Product with Serializable {

    /** A representation of the [[History]] as a `List`. */
    def asList: List[CursorOp]

    override final def toString: String =
      s"""History(${asList.mkString(", ")})"""
  }

  object History {
    private[this] final case class HistoryImpl(override val asList: List[CursorOp]) extends History

    private[Cursor] def fromList(value: List[CursorOp]): History =
      HistoryImpl(value)
  }

  /**
   * A representation of the location of a [[Cursor]], relative to the root of
   * the JSON structure.
   */
  sealed abstract class CursorPath extends Product with Serializable {
    // Developer's note: This is sealed because if we only allow CursorPath
    // instances to be created by Cursor, then we can ensure they are always
    // correct.

    import CursorPath._

    /**
     * The [[CursorPath]] as a list of [[CursorPath#PathElem]] values. The first
     * item in the list is the [[CursorPath#PathElem]] closest to the root of
     * the JSON structure.
     */
    def asList: List[PathElem]

    /**
     * A representation of this [[CursorPath]] as a `String`.
     */
    def pathString: String =
      asList
        .foldLeft(new StringBuilder(asList.size * 3)) {
          case (acc, value) =>
            value match {
              case value: PathElem.JsonObjectKey =>
                acc.append(s".${value.value}")
              case value: PathElem.JsonArrayIndex =>
                acc.append(s"[${value.value}]")
            }
        }
        .toString
  }

  object CursorPath {

    private[this] final case class CursorPathImpl(override val asList: List[PathElem]) extends CursorPath

    private[Cursor] def fromList(value: List[PathElem]): CursorPath =
      CursorPathImpl(value)

    /**
     * A node on the path from the root of the JSON to the current location as
     * described by the [[CursorPath]].
     */
    sealed abstract class PathElem extends Product with Serializable

    object PathElem {

      /**
       * A element in the path from the root to the focus where to focus is on a
       * key/field in a JSON object.
       */
      sealed abstract class JsonObjectKey extends PathElem {

        /** The field/key at this section of the cursor path. */
        def value: String

        override final def toString: String = s"JsonObjectKey(value = ${value})"
      }

      object JsonObjectKey {
        private[this] final case class JsonObjectKeyImpl(override val value: String) extends JsonObjectKey

        private[Cursor] def apply(value: String): JsonObjectKey =
          JsonObjectKeyImpl(value)
      }

      /**
       * A element in the path from the root to the focus where to focus is on an
       * element of a JSON array.
       */
      sealed abstract class JsonArrayIndex extends PathElem {

        /**
         * The index of the array at this section of the cursor path.
         */
        def value: Long

        override final def toString: String = s"JsonArrayIndex(value = $value)"
      }

      object JsonArrayIndex {
        private[this] final case class JsonArrayIndexImpl(override val value: Long) extends JsonArrayIndex

        private[Cursor] def apply(value: Long): JsonArrayIndex =
          JsonArrayIndexImpl(value)
      }
    }
  }

  /**
   * In the event of a failed cursor traversal, this will describe why the
   * failure occurred.
   */
  sealed abstract class CursorFailureReason extends RuntimeException {
    import CursorFailureReason._

    /**
     * The [[FailureTarget]] describing the location at which the failure
     * occurred.
     */
    def failureTarget: FailureTarget

    override final def fillInStackTrace(): Throwable = this
  }

  object CursorFailureReason {
    // Developer's note: As with many types in the Cursor companion object, we
    // do not expose the constructors for the various concrete targets and
    // errors both in the name of binary compatibility and to ensure that
    // instances can only be created in a valid context.
    //
    // The FailureTarget types (not constructors) are exposed so that they can
    // be inspected, but the CursorFailureReason reason types are not. This is
    // because it is expected that the concrete CursorFailureReason types will
    // have a higher volatility than the FailureTarget types.

    /**
     * A failure target is a description of the location of a cursor
     * failure. Usually these locations do not actually exist, for example
     * when you attempt to move to index 0 of an empty array. The failure
     * target is the 0th index, but obviously no such value actually exists,
     * hence the failure.
     */
    sealed abstract class FailureTarget extends Product with Serializable

    object FailureTarget {

      /**
       * A failure when attempting to operate at a given key/field of a JSON
       * object.
       */
      sealed abstract class JsonObjectKeyTarget extends FailureTarget {

        /**
         * The key/field of a JSON object which was the location of the failure.
         */
        def value: String

        override final def toString: String = s"JsonObjectKeyTarget(value = $value)"
      }

      object JsonObjectKeyTarget {
        private[this] final case class JsonObjectKeyTargetImpl(override val value: String) extends JsonObjectKeyTarget

        private[Cursor] def apply(value: String): JsonObjectKeyTarget =
          JsonObjectKeyTargetImpl(value)
      }

      /**
       * A failure when attempting to operate at a given index of a JSON
       * array.
       */
      sealed abstract class JsonArrayIndexTarget extends FailureTarget {

        /**
         * The index of a JSON array which was the location of the failure.
         */
        def value: Long

        override final def toString: String = s"JsonArrayIndexTarget(value = $value)"
      }

      object JsonArrayIndexTarget {
        private[this] final case class JsonArrayIndexTargetImpl(override val value: Long) extends JsonArrayIndexTarget

        private[Cursor] def apply(value: Long): JsonArrayIndexTarget =
          JsonArrayIndexTargetImpl(value)
      }

      /**
       * A special type of failure target describing the attempt to move above the
       * top/root of a JSON value.
       */
      sealed abstract class MoveAboveTopTarget extends FailureTarget {
        override final def toString: String = "MoveAboveTopTarget"
      }

      object MoveAboveTopTarget {
        private[this] case object MoveAboveTopTargetImpl extends MoveAboveTopTarget

        def value: MoveAboveTopTarget = MoveAboveTopTargetImpl
      }

      // Failed relative array operations are special because they have no
      // context with which to describe a target index. That is, attempting to
      // move to index 5 when the focus is an object means the failure target
      // was index 5, but attempting to move "left" when the focus is an
      // object has no concrete failed target index.

      // scalastyle:off
      /**
       * A special type of failure target describing a left relative move in an
       * incorrect context.
       *
       * For example, if the focus is a key/field in a JSON, then calling
       * [[Cursor#left]] would fail because we can't move left in an array if
       * we are in a JSON object. When this happens, we can't use
       * [[JsonArrayIndexTarget]] because that requires a concrete index
       * value at which the failure occurred, but in this case the location
       * of the failure can only be described in relative terms, as opposed
       * to moving left from a JSON array at index 0 which can be described
       * by the index value -1.
       *
       * See the following example for more details.
       *
       * {{{
       * scala> val array: Json = Json.fromValues(List(Json.fromString("a")))
       * val array: io.circe.Json =
       * [
       *   "a"
       * ]
       *
       * scala> val cursor: Cursor = Cursor.fromJson(array)
       * val cursor: io.circe.Cursor =
       * TopCursor(value = [
       *   "a"
       * ])
       *
       * scala> cursor.downArray
       * val res4: io.circe.Cursor = ArrayCursor(value = "a", indexValue = 0, arrayValues = Vector("a"))
       *
       * scala> cursor.downArray.cursorPathString
       * val res5: String = [0]
       *
       * scala> cursor.downArray.left
       * val res6: io.circe.Cursor = FailedCursor(failureReason = Some(io.circe.Cursor$CursorFailureReason$MoveLeftIndexOutOfBoundsFailure$: Attempted to move to index value -1 in JSON array.))
       *
       * scala> cursor.downArray.left.failureReason.map(_.failureTarget)
       * val res7: Option[io.circe.Cursor.CursorFailureReason.FailureTarget] = Some(JsonArrayIndexTarget(value = -1))
       *
       * scala> val jsonObject: Json = Json.fromFields(List("foo" -> Json.fromInt(1)))
       * val jsonObject: io.circe.Json =
       * {
       *   "foo" : 1
       * }
       *
       * scala> val cursor: Cursor = Cursor.fromJson(jsonObject)
       * val cursor: io.circe.Cursor =
       * TopCursor(value = {
       *   "foo" : 1
       * })
       *
       * scala> cursor.downField("foo")
       * val res8: io.circe.Cursor = ObjectCursor(value = 1, obj = object[foo -> 1], keyValue = foo)
       *
       * scala> cursor.downField("foo").cursorPathString
       * val res10: String = .foo
       *
       * scala> cursor.downField("foo").left
       * val res11: io.circe.Cursor = FailedCursor(failureReason = Some(io.circe.Cursor$CursorFailureReason$ExpectedFocusToBeMemberOfJsonArray: Attempted to move in JSON array, but focus was not a member of a JSON array.))
       *
       * scala> cursor.downField("foo").left.failureReason.map(_.failureTarget)
       * val res12: Option[io.circe.Cursor.CursorFailureReason.FailureTarget] = Some(MoveLeftInNonArray)
       * }}}
       */
      sealed abstract class MoveLeftInNonArray extends FailureTarget {
        override final def toString: String = "MoveLeftInNonArray"
      }
      // scalastyle:on

      object MoveLeftInNonArray {
        private[this] case object MoveLeftInNonArrayImpl extends MoveLeftInNonArray

        def value: MoveLeftInNonArray = MoveLeftInNonArrayImpl
      }

      /**
       * A special type of failure target describing a right relative move in an
       * incorrect context.
       *
       * For example, if the focus is a key/field in a JSON, then calling
       * [[Cursor#right]] would fail because we can't move right in an array if
       * we are in a JSON object. When this happens, we can't use
       * [[JsonArrayIndexTarget]] because that requires a concrete index
       * value at which the failure occurred, but in this case the location
       * of the failure can only be described in relative terms, as opposed
       * to moving right from a JSON array at the last element which can be described
       * by the index value of the last element + 1.
       */
      sealed abstract class MoveRightInNonArray extends FailureTarget {
        override final def toString: String = "MoveRightInNonArray"
      }

      object MoveRightInNonArray {
        private[this] case object MoveRightInNonArrayImpl extends MoveRightInNonArray

        def value: MoveRightInNonArray = MoveRightInNonArrayImpl
      }
    }

    /**
     * A failure which occurs when attempting ot move left at the start of a JSON
     * array.
     */
    private[Cursor] final case object MoveLeftIndexOutOfBoundsFailure extends CursorFailureReason {
      override val failureTarget: FailureTarget =
        FailureTarget.JsonArrayIndexTarget(-1L)

      override def getMessage: String = "Attempted to move to index value -1 in JSON array."
    }

    /**
     * A failure which occurs when attempting to move right at the end of a JSON
     * array.
     */
    private[Cursor] final case class MoveRightIndexOutOfBoundsFailure(index: Long) extends CursorFailureReason {
      override def failureTarget: FailureTarget =
        FailureTarget.JsonArrayIndexTarget(index)

      override def getMessage: String = s"Attempted to move to out of bounds array index ${index + 1L}."
    }

    /**
     * A failure which occurs when the focus is required to be a member of a JSON
     * array, but it is not.
     */
    private[Cursor] final case class ExpectedFocusToBeMemberOfJsonArray(override val failureTarget: FailureTarget)
        extends CursorFailureReason {

      override def getMessage: String = "Attempted to move in JSON array, but focus was not a member of a JSON array."
    }

    /**
     * A failure which occurs when attempting to move up from the top/root of a
     * JSON structure.
     */
    private[Cursor] case object MoveUpFromTop extends CursorFailureReason {
      override def failureTarget: FailureTarget =
        FailureTarget.MoveAboveTopTarget.value

      override def getMessage: String = "Attempted to move up from TopCursor. There is no up from TopCursor."
    }

    /**
     * A failure which occurs when the focus type is incorrect for the
     * operation. For example, [[Cursor#downArray]] requires the focus to be
     * a JSON array or [[Cursor#downField]] requires the focus to be a JSON
     * object.
     */
    private[Cursor] final case class IncorrectFocusTypeForNavigationOperation(
      expected: JsonTypeDescription,
      actual: JsonTypeDescription,
      override val failureTarget: FailureTarget
    ) extends CursorFailureReason {
      override def getMessage: String =
        s"Unable to navigate at focus. Expected JSON of type ${expected.value}, but got ${actual.value}."
    }

    /**
     * A failure which occurs when attempting to move down into a JSON array at
     * an index which doesn't exist.
     */
    private[Cursor] final case class DownArrayOutOfBounds(n: Int, actualSize: Int) extends CursorFailureReason {
      override def failureTarget: FailureTarget =
        FailureTarget.JsonArrayIndexTarget(n.toLong)

      // scalastyle:off
      override def getMessage: String =
        s"Attempted to enter into JSON array at index ${n}, but that is out of bounds for the array of size ${actualSize}."
      // scalastyle:on
    }

    /**
     * A failure which occurs when attempting to move to a sibling key/field in a
     * JSON object, but no such sibling exists.
     */
    private[Cursor] final case class NoSiblingWithMatchingKeyName(targetFocusKey: String) extends CursorFailureReason {
      override def failureTarget: FailureTarget =
        FailureTarget.JsonObjectKeyTarget(targetFocusKey)

      override def getMessage: String =
        s"""Attempted to navigate to sibling in JSON object with key name "$targetFocusKey", but no such key exists."""
    }

    /**
     * A failure which occurs when attempting to move a sibling in the
     * [[TopCursor]].
     */
    private[Cursor] final case class NoSiblingsInTopContext(targetFocusKey: String) extends CursorFailureReason {
      override def failureTarget: FailureTarget =
        FailureTarget.JsonObjectKeyTarget(targetFocusKey)

      // scalastyle:off
      override def getMessage: String =
        s"""Attempted to navigate to sibling in JSON object with key name "${targetFocusKey}", but focus was the Root of the JSON structure, not an object member."""
      // scalastyle:on
    }

    /**
     * A failure which occurs when attempting to move to a sibling when the focus
     * is a member of a JSON array.
     */
    private[Cursor] final case class NoSiblingsInArrayContext(targetFocusKey: String) extends CursorFailureReason {
      override def failureTarget: FailureTarget =
        FailureTarget.JsonObjectKeyTarget(targetFocusKey)

      // scalastyle:off
      override def getMessage: String =
        s"""Attempted to navigate to sibling in JSON object with key name "${targetFocusKey}", but focus was a member of a JSON array, not an object."""
      // scalastyle:on
    }

    /**
     * A failure which occurs when attempting to navigate to a key/field in a
     * JSON object, but that key/field doesn't exist.
     */
    private[Cursor] final case class MissingKeyInObject(targetFocusKey: String) extends CursorFailureReason {
      override def failureTarget: FailureTarget =
        FailureTarget.JsonObjectKeyTarget(targetFocusKey)

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
