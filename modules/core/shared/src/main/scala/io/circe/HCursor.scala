package io.circe

import cats.Applicative
import cats.kernel.Eq
import scala.annotation.tailrec

sealed abstract class HCursor extends ACursor {
  def value: Json
  protected def lastCursor: HCursor
  protected def lastOp: HistoryOp

  final def history: List[HistoryOp] = {
    var next = this
    val builder = List.newBuilder[HistoryOp]

    while (next.ne(null)) {
      if (next.lastOp.ne(null)) {
        builder += next.lastOp
      }
      next = next.lastCursor
    }

    builder.result()
  }

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

  final def downAt(p: Json => Boolean): ACursor = downArray.find(p)

  final def downN(n: Int): ACursor = downArray.rightN(n)

  /**
   * Create a new cursor that has failed on the given operation.
   *
   * @group Utilities
   */
  protected def fail(op: CursorOp): ACursor
}

final object HCursor {
  def fromJson(value: Json): HCursor = new ValueCursor(value) {
    def lastCursor: HCursor = null
    def lastOp: HistoryOp = null
  }

  private[this] val eqJsonList: Eq[List[Json]] = cats.instances.list.catsKernelStdEqForList[Json]

  implicit val eqHCursor: Eq[HCursor] = new Eq[HCursor] {
    def eqv(a: HCursor, b: HCursor): Boolean =
      Json.eqJson.eqv(a.value, b.value) &&
      ((a.lastOp.eq(null) && b.lastOp.eq(null)) || HistoryOp.eqHistoryOp.eqv(a.lastOp, b.lastOp)) &&
      ((a.lastCursor.eq(null) && b.lastCursor.eq(null)) || eqv(a.lastCursor, b.lastCursor)) && (
        (a, b) match {
          case (_: ValueCursor, _: ValueCursor) => true
          case (aa: ArrayCursor, ba: ArrayCursor) =>
            aa.changed == ba.changed &&
            eqv(aa.parent, ba.parent) &&
            eqJsonList.eqv(aa.ls, ba.ls) &&
            eqJsonList.eqv(aa.rs, ba.rs)
          case (ao: ObjectCursor, bo: ObjectCursor) =>
            ao.changed == bo.changed &&
            eqv(ao.parent, bo.parent) &&
            ao.key == bo.key &&
            JsonObject.eqJsonObject.eqv(ao.obj, bo.obj)
          case _ => false
        }
      )
  }

  private[this] sealed abstract class FailedCursor extends ACursor {
    final def succeeded: Boolean = false
    final def success: Option[HCursor] = None

    final def focus: Option[Json] = None
    final def top: Option[Json] = None

    final def withFocus(f: Json => Json): ACursor = this
    final def withFocusM[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[ACursor] = F.pure(this)

    final def fieldSet: Option[Set[String]] = None
    final def fields: Option[List[String]] = None
    final def lefts: Option[List[Json]] = None
    final def rights: Option[List[Json]] = None

    final def downArray: ACursor = this
    final def downAt(p: Json => Boolean): ACursor = this
    final def downField(k: String): ACursor = this
    final def downN(n: Int): ACursor = this
    final def find(p: Json => Boolean): ACursor = this
    final def leftAt(p: Json => Boolean): ACursor = this
    final def leftN(n: Int): ACursor = this
    final def rightAt(p: Json => Boolean): ACursor = this
    final def rightN(n: Int): ACursor = this
    final def up: ACursor = this

    final def left: ACursor = this
    final def right: ACursor = this
    final def first: ACursor = this
    final def last: ACursor = this

    final def delete: ACursor = this
    final def deleteGoLeft: ACursor = this
    final def deleteGoRight: ACursor = this
    final def deleteGoFirst: ACursor = this
    final def deleteGoLast: ACursor = this
    final def deleteLefts: ACursor = this
    final def deleteRights: ACursor = this

    final def setLefts(x: List[Json]): ACursor = this
    final def setRights(x: List[Json]): ACursor = this

    final def field(k: String): ACursor = this
    final def deleteGoField(q: String): ACursor = this

    final def reattempt: ACursor = this
  }

  private[this] sealed abstract class BaseHCursor extends HCursor { self =>
    final def top: Option[Json] = {
      @tailrec
      def go(c: HCursor): Json = c match {
        case v: ValueCursor => v.value
        case a: ArrayCursor =>
          val newValue = Json.fromValues((a.value :: a.rs).reverse_:::(a.ls))

          go(
            a.parent match {
              case pv: ValueCursor => new ValueCursor(newValue) {
                def lastCursor: HCursor = null
                def lastOp: HistoryOp = null
              }
              case pa: ArrayCursor => new ArrayCursor(newValue, pa.parent, a.changed || pa.changed, pa.ls, pa.rs) {
                def lastCursor: HCursor = null
                def lastOp: HistoryOp = null
              }
              case po: ObjectCursor => new ObjectCursor(
                newValue,
                po.parent,
                a.changed || po.changed,
                po.key,
                if (a.changed) po.obj.add(po.key, newValue) else po.obj
              ) {
                def lastCursor: HCursor = null
                def lastOp: HistoryOp = null
              }
            }
          )
        case o: ObjectCursor =>
          val newValue = Json.fromJsonObject(if (o.changed) o.obj.add(o.key, o.value) else o.obj)

          go(
            o.parent match {
              case pv: ValueCursor => new ValueCursor(newValue) {
                def lastCursor: HCursor = null
                def lastOp: HistoryOp = null
              }
              case pa: ArrayCursor => new ArrayCursor(newValue, pa.parent, o.changed || pa.changed, pa.ls, pa.rs) {
                def lastCursor: HCursor = null
                def lastOp: HistoryOp = null
              }
              case po: ObjectCursor => new ObjectCursor(
                newValue,
                po.parent,
                o.changed || po.changed,
                po.key,
                po.obj
              ) {
                def lastCursor: HCursor = null
                def lastOp: HistoryOp = null
              }
            }
          )
      }

      Some(go(this))
    }

    final def downArray: ACursor = value match {
      case Json.JArray(h :: t) =>
        new ArrayCursor(h, this, false, Nil, t) {
          def lastCursor: HCursor = self
          def lastOp: HistoryOp = HistoryOp.ok(CursorOp.DownArray)
        }
      case _ => fail(CursorOp.DownArray)
    }

    final def downField(k: String): ACursor = value match {
      case Json.JObject(o) =>
        val m = o.toMap

        if (!m.contains(k)) fail(CursorOp.DownField(k)) else {
          new ObjectCursor(m(k), this, false, k, o) {
            def lastCursor: HCursor = self
            def lastOp: HistoryOp = HistoryOp.ok(CursorOp.DownField(k))
          }
        }
      case _ => fail(CursorOp.DownField(k))
    }

    protected final def fail(op: CursorOp): ACursor = {
      val incorrectFocus: Boolean = (op.requiresObject && !value.isObject) || (op.requiresArray && !value.isArray)

      new FailedCursor {
        def history: List[HistoryOp] = self.history :+ HistoryOp.fail(op, incorrectFocus)
      }
    }
  }

  private[this] abstract class ValueCursor(final val value: Json) extends BaseHCursor { self =>
    final def withFocus(f: Json => Json): ACursor = new ValueCursor(f(value)) {
      def lastCursor: HCursor = self
      def lastOp: HistoryOp = null
    }

    final def withFocusM[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[ACursor] =
      F.map(f(value))(newValue =>
        new ValueCursor(newValue) {
          def lastCursor: HCursor = self
          def lastOp: HistoryOp = null
        }
      )

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

    final def reattempt: ACursor = new ValueCursor(value) {
      def lastCursor: HCursor = self
      def lastOp: HistoryOp = HistoryOp.reattempt
    }
  }

  private[this] abstract class ArrayCursor(
    final val value: Json,
    final val parent: HCursor,
    final val changed: Boolean,
    final val ls: List[Json],
    final val rs: List[Json]
  ) extends BaseHCursor { self =>
    final def withFocus(f: Json => Json): ACursor = new ArrayCursor(f(value), parent, changed = true, ls, rs) {
      def lastCursor: HCursor = self
      def lastOp: HistoryOp = null
    }

    final def withFocusM[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[ACursor] =
      F.map(f(value))(newValue =>
        new ArrayCursor(newValue, parent, changed = true, ls, rs) {
          def lastCursor: HCursor = self
          def lastOp: HistoryOp = null
        }
      )

    final def up: ACursor = {
      val newValue = Json.fromValues((value :: rs).reverse_:::(ls))

      parent match {
        case v: ValueCursor =>  new ValueCursor(newValue) {
          def lastCursor: HCursor = self
          def lastOp: HistoryOp = HistoryOp.ok(CursorOp.MoveUp)
        }
        case a: ArrayCursor => new ArrayCursor(newValue, a.parent, changed || a.changed, a.ls, a.rs) {
          def lastCursor: HCursor = self
          def lastOp: HistoryOp = HistoryOp.ok(CursorOp.MoveUp)
        }
        case o: ObjectCursor => new ObjectCursor(
          newValue,
          o.parent,
          changed || o.changed,
          o.key,
          if (changed) o.obj.add(o.key, newValue) else o.obj
        ) {
          def lastCursor: HCursor = self
          def lastOp: HistoryOp = HistoryOp.ok(CursorOp.MoveUp)
        }
      }
    }

    final def delete: ACursor = {
      val newValue = Json.fromValues(rs.reverse_:::(ls))

      parent match {
        case v: ValueCursor => new ValueCursor(newValue) {
          def lastCursor: HCursor = self
          def lastOp: HistoryOp = HistoryOp.ok(CursorOp.DeleteGoParent)
        }
        case a: ArrayCursor => new ArrayCursor(newValue, a.parent, true, a.ls, a.rs) {
          def lastCursor: HCursor = self
          def lastOp: HistoryOp = HistoryOp.ok(CursorOp.DeleteGoParent)
        }
        case o: ObjectCursor => new ObjectCursor(
          newValue,
          o.parent,
          true,
          o.key,
          o.obj
        ) {
          def lastCursor: HCursor = self
          def lastOp: HistoryOp = HistoryOp.ok(CursorOp.DeleteGoParent)
        }
      }
    }

    final def lefts: Option[List[Json]] = Some(ls)
    final def rights: Option[List[Json]] = Some(rs)

    final def left: ACursor = ls match {
      case h :: t => new ArrayCursor(h, parent, changed, t, value :: rs) {
        def lastCursor: HCursor = self
        def lastOp: HistoryOp = HistoryOp.ok(CursorOp.MoveLeft)
      }
      case Nil => fail(CursorOp.MoveLeft)
    }

    final def right: ACursor = rs match {
      case h :: t => new ArrayCursor(h, parent, changed, value :: ls, t) {
        def lastCursor: HCursor = self
        def lastOp: HistoryOp = HistoryOp.ok(CursorOp.MoveRight)
      }
      case Nil => fail(CursorOp.MoveRight)
    }

    final def first: ACursor = (value :: rs).reverse_:::(ls) match {
      case h :: t => new ArrayCursor(h, parent, changed, Nil, t) {
        def lastCursor: HCursor = self
        def lastOp: HistoryOp = HistoryOp.ok(CursorOp.MoveFirst)
      }
      case Nil => fail(CursorOp.MoveFirst)
    }

    final def last: ACursor = (value :: ls).reverse_:::(rs) match {
      case h :: t => new ArrayCursor(h, parent, changed, t, Nil) {
        def lastCursor: HCursor = self
        def lastOp: HistoryOp = HistoryOp.ok(CursorOp.MoveLast)
      }
      case Nil => fail(CursorOp.MoveLast)
    }

    final def deleteGoLeft: ACursor = ls match {
      case h :: t => new ArrayCursor(h, parent, true, t, rs) {
        def lastCursor: HCursor = self
        def lastOp: HistoryOp = HistoryOp.ok(CursorOp.DeleteGoLeft)
      }
      case Nil => fail(CursorOp.DeleteGoLeft)
    }

    final def deleteGoRight: ACursor = rs match {
      case h :: t => new ArrayCursor(h, parent, true, ls, t) {
        def lastCursor: HCursor = self
        def lastOp: HistoryOp = HistoryOp.ok(CursorOp.DeleteGoRight)
      }
      case Nil => fail(CursorOp.DeleteGoRight)
    }

    final def deleteGoFirst: ACursor = rs.reverse_:::(ls) match {
      case h :: t => new ArrayCursor(h, parent, true, Nil, t) {
        def lastCursor: HCursor = self
        def lastOp: HistoryOp = HistoryOp.ok(CursorOp.DeleteGoFirst)
      }
      case Nil => fail(CursorOp.DeleteGoFirst)
    }

    final def deleteGoLast: ACursor = ls.reverse_:::(rs) match {
      case h :: t => new ArrayCursor(h, parent, true, t, Nil) {
        def lastCursor: HCursor = self
        def lastOp: HistoryOp = HistoryOp.ok(CursorOp.DeleteGoLast)
      }
      case Nil => fail(CursorOp.DeleteGoLast)
    }

    final def deleteLefts: ACursor = new ArrayCursor(value, parent, true, Nil, rs) {
      def lastCursor: HCursor = self
      def lastOp: HistoryOp = HistoryOp.ok(CursorOp.DeleteLefts)
    }

    final def deleteRights: ACursor = new ArrayCursor(value, parent, true, ls, Nil) {
      def lastCursor: HCursor = self
      def lastOp: HistoryOp = HistoryOp.ok(CursorOp.DeleteRights)
    }

    final def setLefts(js: List[Json]): ACursor = new ArrayCursor(value, parent, true, js, rs) {
      def lastCursor: HCursor = self
      def lastOp: HistoryOp = HistoryOp.ok(CursorOp.SetLefts(js))
    }

    final def setRights(js: List[Json]): ACursor = new ArrayCursor(value, parent, true, ls, js) {
      def lastCursor: HCursor = self
      def lastOp: HistoryOp = HistoryOp.ok(CursorOp.SetRights(js))
    }

    final def field(k: String): ACursor = fail(CursorOp.Field(k))
    final def deleteGoField(k: String): ACursor = fail(CursorOp.DeleteGoField(k))

    final def reattempt: ACursor = new ArrayCursor(value, parent, changed, rs, ls) {
      def lastCursor: HCursor = self
      def lastOp: HistoryOp = HistoryOp.reattempt
    }
  }

  private[this] sealed abstract class ObjectCursor(
    final val value: Json,
    final val parent: HCursor,
    final val changed: Boolean,
    final val key: String,
    final val obj: JsonObject
  ) extends BaseHCursor { self =>
    final def withFocus(f: Json => Json): ACursor = new ObjectCursor(f(value), parent, true, key, obj) {
      def lastCursor: HCursor = self
      def lastOp: HistoryOp = null
    }

    final def withFocusM[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[ACursor] =
      F.map(f(value))(newValue =>
        new ObjectCursor(newValue, parent, true, key, obj) {
          def lastCursor: HCursor = self
          def lastOp: HistoryOp = null
        }
      )

    final def lefts: Option[List[Json]] = None
    final def rights: Option[List[Json]] = None

    final def up: ACursor = {
      val newValue = Json.fromJsonObject(if (changed) obj.add(key, value) else obj)

      parent match {
        case v: ValueCursor => new ValueCursor(newValue) {
          def lastCursor: HCursor = self
          def lastOp: HistoryOp = HistoryOp.ok(CursorOp.MoveUp)
        }
        case a: ArrayCursor => new ArrayCursor(newValue, a.parent, changed || a.changed, a.ls, a.rs) {
          def lastCursor: HCursor = self
          def lastOp: HistoryOp = HistoryOp.ok(CursorOp.MoveUp)
        }
        case o: ObjectCursor => new ObjectCursor(newValue, o.parent, changed || o.changed, o.key, o.obj) {
          def lastCursor: HCursor = self
          def lastOp: HistoryOp = HistoryOp.ok(CursorOp.MoveUp)
        }
      }
    }

    final def delete: ACursor = {
      val newValue = Json.fromJsonObject(obj.remove(key))

      parent match {
        case v: ValueCursor => new ValueCursor(newValue) {
          def lastCursor: HCursor = self
          def lastOp: HistoryOp = HistoryOp.ok(CursorOp.DeleteGoParent)
        }
        case a: ArrayCursor => new ArrayCursor(newValue, a.parent, true, a.ls, a.rs) {
          def lastCursor: HCursor = self
          def lastOp: HistoryOp = HistoryOp.ok(CursorOp.DeleteGoParent)
        }
        case o: ObjectCursor => new ObjectCursor(newValue, o.parent, true, o.key, o.obj) {
          def lastCursor: HCursor = self
          def lastOp: HistoryOp = HistoryOp.ok(CursorOp.DeleteGoParent)
        }
      }
    }

    final def field(k: String): ACursor = {
      val m = obj.toMap

      if (m.contains(k)) new ObjectCursor(m(k), parent, changed, k, obj) {
        def lastCursor: HCursor = self
        def lastOp: HistoryOp = HistoryOp.ok(CursorOp.Field(k))
      } else fail(CursorOp.Field(k))
    }

    final def deleteGoField(k: String): ACursor = {
      val m = obj.toMap

      if (m.contains(k)) new ObjectCursor(m(k), parent, true, k, obj.remove(key)) {
        def lastCursor: HCursor = self
        def lastOp: HistoryOp = HistoryOp.ok(CursorOp.DeleteGoField(k))
      } else fail(CursorOp.DeleteGoField(k))
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

    final def reattempt: ACursor = new ObjectCursor(value, parent, changed, key, obj) {
      def lastCursor: HCursor = self
      def lastOp: HistoryOp = HistoryOp.reattempt
    }
  }
}
