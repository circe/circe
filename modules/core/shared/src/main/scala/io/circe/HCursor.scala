package io.circe

import cats.Applicative
import scala.annotation.tailrec

sealed abstract class HCursor extends ACursor { self =>
  def value: Json

  final def succeeded: Boolean = true

  final def success: Option[HCursor] = Some(this)

  final def focus: Option[Json] = Some(value)

  final def fieldSet: Option[Set[String]] = value match {
    case Json.JObject(o) => Some(o.fieldSet)
    case _ => None
  }

  final def fields: Option[List[String]] = value match {
    case Json.JObject(o) => Some(o.fields)
    case _ => None
  }

  final def top: Option[Json] = {
    @tailrec
    def go(c: HCursor): Json = c match {
      case v: HCursor.ValueCursor => v.value
      case a: HCursor.ArrayCursor =>
        val newValue = Json.fromValues((a.value :: a.rs).reverse_:::(a.ls))

        go(
          a.parent match {
            case pv: HCursor.ValueCursor => pv.copy(value = newValue)
            case pa: HCursor.ArrayCursor => pa.copy(value = newValue, history = Nil, changed = a.changed || pa.changed)
            case po: HCursor.ObjectCursor => po.copy(
              value = newValue,
              history = Nil,
              changed = a.changed || po.changed,
              obj = if (a.changed) po.obj.add(po.key, newValue) else po.obj
            )
          }
        )
      case o: HCursor.ObjectCursor =>
        val newValue = Json.fromJsonObject(if (o.changed) o.obj.add(o.key, o.value) else o.obj)

        go(
          o.parent match {
            case pv: HCursor.ValueCursor => pv.copy(value = newValue)
            case pa: HCursor.ArrayCursor => pa.copy(value = newValue, history = Nil, changed = o.changed || pa.changed)
            case po: HCursor.ObjectCursor => po.copy(value = newValue, history = Nil, changed = o.changed || po.changed)
          }
        )
    }

    Some(go(this))
  }

  final def leftN(n: Int): ACursor = if (n < 0) rightN(-n) else {
    @tailrec
    def go(i: Int, c: ACursor): ACursor = if (i == 0) c else go(i - 1, c.left)

    go(n, this)
  }

  final def rightN(n: Int): ACursor = if (n < 0) leftN(-n) else {
    @tailrec
    def go(i: Int, c: ACursor): ACursor = if (i == 0) c else go(i - 1, c.right)

    go(n, this)
  }

  final def leftAt(p: Json => Boolean): ACursor = {
    @tailrec
    def go(c: ACursor): ACursor = c match {
      case success: HCursor => if (p(success.value)) success else go(success.left)
      case other => other
    }

    go(left)
  }

  final def rightAt(p: Json => Boolean): ACursor = right.find(p)

  final def find(p: Json => Boolean): ACursor = {
    @annotation.tailrec
    def go(c: ACursor): ACursor = c match {
      case success: HCursor => if (p(success.value)) success else go(success.right)
      case other => other
    }

    go(this)
  }

  final def downArray: ACursor = value match {
    case Json.JArray(h :: t) =>
      HCursor.ArrayCursor(h, HistoryOp.ok(CursorOp.DownArray) :: history, this, false, Nil, t)
    case _ => fail(CursorOp.DownArray)
  }

  final def downAt(p: Json => Boolean): ACursor = downArray.find(p)

  final def downN(n: Int): ACursor = downArray.rightN(n)

  final def downField(k: String): ACursor = value match {
    case Json.JObject(o) =>
      val m = o.toMap

      if (!m.contains(k)) fail(CursorOp.DownField(k)) else {
        HCursor.ObjectCursor(m(k), HistoryOp.ok(CursorOp.DownField(k)) :: history, this, false, k, o)
      }
    case _ => fail(CursorOp.DownField(k))
  }

  /**
   * Create a new cursor that has failed on the given operation.
   *
   * @group Utilities
   */
  protected final def fail(op: CursorOp): ACursor = {
    val incorrectFocus: Boolean = (op.requiresObject && !value.isObject) || (op.requiresArray && !value.isArray)

    FailedCursor(HistoryOp.fail(op, incorrectFocus) :: history)
  }
}

final object HCursor {
  def fromJson(value: Json): HCursor = ValueCursor(value, Nil)

  private[circe] final case class ValueCursor(value: Json, history: List[HistoryOp]) extends HCursor { self =>
    final def withFocus(f: Json => Json): ACursor = copy(value = f(value))

    final def withFocusM[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[ACursor] =
      F.map(f(value))(newValue => copy(value = newValue))

    final def lefts: Option[List[Json]] = None
    final def rights: Option[List[Json]] = None

    final def up: ACursor = fail(CursorOp.MoveUp)
    final def delete: ACursor = fail(CursorOp.DeleteGoParent)

    final def left: ACursor = fail(CursorOp.MoveLeft)
    final def right: ACursor = fail(CursorOp.MoveRight)
    final def first: ACursor = fail(CursorOp.MoveFirst)
    final def last: ACursor = fail(CursorOp.MoveLast)

    final def deleteGoLeft: ACursor = fail(CursorOp.DeleteGoLeft)
    final def deleteGoRight: ACursor = fail(CursorOp.DeleteGoRight)
    final def deleteGoFirst: ACursor = fail(CursorOp.DeleteGoFirst)
    final def deleteGoLast: ACursor = fail(CursorOp.DeleteGoLast)
    final def deleteLefts: ACursor = fail(CursorOp.DeleteLefts)
    final def deleteRights: ACursor = fail(CursorOp.DeleteRights)

    final def setLefts(x: List[Json]): ACursor = fail(CursorOp.SetLefts(x))
    final def setRights(x: List[Json]): ACursor = fail(CursorOp.SetRights(x))

    final def field(k: String): ACursor = fail(CursorOp.Field(k))
    final def deleteGoField(k: String): ACursor = fail(CursorOp.DeleteGoField(k))

    final def reattempt: ACursor = ValueCursor(value, HistoryOp.reattempt :: history)
  }

  private[circe] final case class ArrayCursor(
    value: Json,
    history: List[HistoryOp],
    parent: HCursor,
    changed: Boolean,
    ls: List[Json],
    rs: List[Json]
  ) extends HCursor { self =>
    final def withFocus(f: Json => Json): ACursor = copy(value = f(value), changed = true)

    final def withFocusM[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[ACursor] =
      F.map(f(value))(newValue => copy(value = newValue, changed = true))

    final def up: ACursor = {
      val newValue = Json.fromValues((value :: rs).reverse_:::(ls))
      val newHistory = HistoryOp.ok(CursorOp.MoveUp) :: self.history

      parent match {
        case v: ValueCursor => ValueCursor(newValue, newHistory)
        case a: ArrayCursor => a.copy(value = newValue, history = newHistory, changed = changed || a.changed)
        case o: ObjectCursor => o.copy(
          value = newValue,
          history = newHistory,
          changed = changed || o.changed,
          obj = if (changed) o.obj.add(o.key, newValue) else o.obj
        )
      }
    }

    def delete: ACursor = {
      val newValue = Json.fromValues(rs.reverse_:::(ls))
      val newHistory = HistoryOp.ok(CursorOp.DeleteGoParent) :: self.history

      parent match {
        case v: ValueCursor => v.copy(value = newValue, history = newHistory)
        case a: ArrayCursor => a.copy(value = newValue, history = newHistory, changed = true)
        case o: ObjectCursor => o.copy(value = newValue, history = newHistory, changed = true)
      }
    }

    def lefts: Option[List[Json]] = Some(ls)
    def rights: Option[List[Json]] = Some(rs)

    def left: ACursor = ls match {
      case h :: t => copy(value = h, history = HistoryOp.ok(CursorOp.MoveLeft) :: history, ls = t, rs = value :: rs)
      case Nil => fail(CursorOp.MoveLeft)
    }

    def right: ACursor = rs match {
      case h :: t => copy(value = h, history = HistoryOp.ok(CursorOp.MoveRight) :: history, ls = value :: ls, rs = t)
      case Nil => fail(CursorOp.MoveRight)
    }

    def first: ACursor = (value :: rs).reverse_:::(ls) match {
      case h :: t => copy(value = h, history = HistoryOp.ok(CursorOp.MoveFirst) :: history, ls = Nil, rs = t)
      case Nil => fail(CursorOp.MoveFirst)
    }

    def last: ACursor = (value :: ls).reverse_:::(rs) match {
      case h :: t => copy(value = h, history = HistoryOp.ok(CursorOp.MoveLast) :: history, ls = t, rs = Nil)
      case Nil => fail(CursorOp.MoveLast)
    }

    def deleteGoLeft: ACursor = ls match {
      case h :: t => copy(value = h, history = HistoryOp.ok(CursorOp.DeleteGoLeft) :: history, changed = true, ls = t)
      case Nil => fail(CursorOp.DeleteGoLeft)
    }

    def deleteGoRight: ACursor = rs match {
      case h :: t => copy(value = h, history = HistoryOp.ok(CursorOp.DeleteGoRight) :: history, changed = true, rs = t)
      case Nil => fail(CursorOp.DeleteGoRight)
    }

    def deleteGoFirst: ACursor = rs.reverse_:::(ls) match {
      case h :: t => copy(
        value = h,
        history = HistoryOp.ok(CursorOp.DeleteGoFirst) :: history,
        changed = true,
        ls = Nil,
        rs = t
      )
      case Nil => fail(CursorOp.DeleteGoFirst)
    }

    def deleteGoLast: ACursor = ls.reverse_:::(rs) match {
      case h :: t => copy(
        value = h,
        history = HistoryOp.ok(CursorOp.DeleteGoLast) :: history,
        changed = true,
        ls = t,
        rs = Nil
      )
      case Nil => fail(CursorOp.DeleteGoLast)
    }

    def deleteLefts: ACursor = copy(history = HistoryOp.ok(CursorOp.DeleteLefts) :: history, changed = true, ls = Nil)
    def deleteRights: ACursor = copy(history = HistoryOp.ok(CursorOp.DeleteRights) :: history, changed = true, rs = Nil)
    def setLefts(js: List[Json]): ACursor = copy(
      history = HistoryOp.ok(CursorOp.SetLefts(js)) :: history,
      changed = true,
      ls = js
    )
    def setRights(js: List[Json]): ACursor = copy(
      history = HistoryOp.ok(CursorOp.SetRights(js)) :: history,
      changed = true,
      rs = js
    )

    def field(k: String): ACursor = fail(CursorOp.Field(k))
    def deleteGoField(k: String): ACursor = fail(CursorOp.DeleteGoField(k))

    final def reattempt: ACursor = copy(history = HistoryOp.reattempt :: history)
  }

  private[circe] final case class ObjectCursor(
    value: Json,
    history: List[HistoryOp],
    parent: HCursor,
    changed: Boolean,
    key: String,
    obj: JsonObject
  ) extends HCursor { self =>
    final def withFocus(f: Json => Json): ACursor = copy(value = f(value), changed = true)

    final def withFocusM[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[ACursor] =
      F.map(f(value))(newValue => copy(value = newValue, changed = true))

    final def lefts: Option[List[Json]] = None
    final def rights: Option[List[Json]] = None

    def up: ACursor = {
      val newValue = Json.fromJsonObject(if (changed) obj.add(key, value) else obj)
      val newHistory = HistoryOp.ok(CursorOp.MoveUp) :: self.history

      parent match {
        case v: ValueCursor => v.copy(value = newValue, history = newHistory)
        case a: ArrayCursor => a.copy(value = newValue, history = newHistory, changed = changed || a.changed)
        case o: ObjectCursor => o.copy(value = newValue, history = newHistory, changed = changed || o.changed)
      }
    }

    def delete: ACursor = {
      val newValue = Json.fromJsonObject(obj.remove(key))
      val newHistory = HistoryOp.ok(CursorOp.DeleteGoParent) :: self.history

      parent match {
        case v: ValueCursor => v.copy(value = newValue, history = newHistory)
        case a: ArrayCursor => a.copy(value = newValue, history = newHistory, changed = true)
        case o: ObjectCursor => o.copy(value = newValue, history = newHistory, changed = true)
      }
    }

    def field(k: String): ACursor = {
      val m = obj.toMap

      if (m.contains(k)) copy(
        value = m(k),
        history = HistoryOp.ok(CursorOp.Field(k)) :: history,
        key = k
      ) else fail(CursorOp.Field(k))
    }

    def deleteGoField(k: String): ACursor = {
      val m = obj.toMap

      if (m.contains(k)) copy(
        value = m(k),
        history = HistoryOp.ok(CursorOp.DeleteGoField(k)) :: history,
        changed = true,
        key = k,
        obj = obj.remove(key)
      ) else fail(CursorOp.DeleteGoField(k))
    }

    final def left: ACursor = fail(CursorOp.MoveLeft)
    final def right: ACursor = fail(CursorOp.MoveRight)
    final def first: ACursor = fail(CursorOp.MoveFirst)
    final def last: ACursor = fail(CursorOp.MoveLast)

    final def deleteGoLeft: ACursor = fail(CursorOp.DeleteGoLeft)
    final def deleteGoRight: ACursor = fail(CursorOp.DeleteGoRight)
    final def deleteGoFirst: ACursor = fail(CursorOp.DeleteGoFirst)
    final def deleteGoLast: ACursor = fail(CursorOp.DeleteGoLast)
    final def deleteLefts: ACursor = fail(CursorOp.DeleteLefts)
    final def deleteRights: ACursor = fail(CursorOp.DeleteRights)

    final def setLefts(x: List[Json]): ACursor = fail(CursorOp.SetLefts(x))
    final def setRights(x: List[Json]): ACursor = fail(CursorOp.SetRights(x))

    final def reattempt: ACursor = copy(history = HistoryOp.reattempt :: history)
  }
}
