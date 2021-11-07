package io.circe

import cats.Applicative
import io.circe.cursor.{ ArrayCursor, ObjectCursor, TopCursor }
import scala.annotation.tailrec

abstract class HCursor(lastCursor: HCursor, lastOp: CursorOp) extends ACursor(lastCursor, lastOp) {
  def value: Json

  def replace(newValue: Json, cursor: HCursor, op: CursorOp): HCursor
  def addOp(cursor: HCursor, op: CursorOp): HCursor

  final def withFocus(f: Json => Json): ACursor = replace(f(value), this, null)
  final def withFocusM[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[ACursor] =
    F.map(f(value))(replace(_, this, null))

  final def succeeded: Boolean = true
  final def success: Option[HCursor] = Some(this)

  final def focus: Option[Json] = Some(value)

  final def values: Option[Iterable[Json]] = value match {
    case Json.JArray(vs) => Some(vs)
    case _               => None
  }

  final def keys: Option[Iterable[String]] = value match {
    case Json.JObject(o) => Some(o.keys)
    case _               => None
  }

  final def top: Option[Json] = {
    var current: HCursor = this

    while (!current.isInstanceOf[TopCursor]) {
      current = current.up.asInstanceOf[HCursor]
    }

    Some(current.asInstanceOf[TopCursor].value)
  }

  override final def root: HCursor = {
    var current: HCursor = this

    while (!current.isInstanceOf[TopCursor]) {
      current = current.up.asInstanceOf[HCursor]
    }

    current
  }

  final def downArray: ACursor = value match {
    case Json.JArray(values) if !values.isEmpty =>
      new ArrayCursor(values, 0, this, false)(this, CursorOp.DownArray)
    case _ => fail(CursorOp.DownArray)
  }

  final def find(p: Json => Boolean): ACursor = {
    @tailrec
    def go(c: ACursor): ACursor = c match {
      case success: HCursor => if (p(success.value)) success else go(success.right)
      case other            => other
    }

    go(this)
  }

  final def downField(k: String): ACursor = value match {
    case Json.JObject(o) =>
      if (!o.contains(k)) fail(CursorOp.DownField(k))
      else {
        new ObjectCursor(o, k, this, false)(this, CursorOp.DownField(k))
      }
    case _ => fail(CursorOp.DownField(k))
  }

  final def downN(n: Int): ACursor = value match {
    case Json.JArray(values) if n >= 0 && values.size > n =>
      new ArrayCursor(values, n, this, false)(this, CursorOp.DownN(n))
    case _ => fail(CursorOp.DownN(n))
  }

  /**
   * Create a new cursor that has failed on the given operation.
   *
   * @group Utilities
   */
  protected[this] final def fail(op: CursorOp): ACursor = new FailedCursor(this, op)
}

object HCursor {
  def fromJson(value: Json): HCursor = new TopCursor(value)(null, null)
}
