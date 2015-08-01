package io.jfc

import algebra.Eq
import cats.Show
import cats.std.list._

sealed class Context(val toList: List[ContextElement]) {
  def +:(e: ContextElement): Context = new Context(e :: toList)
}

object Context {
  def empty: Context = new Context(Nil)

  implicit val eqContext: Eq[Context] = Eq.by(_.toList)
  implicit val showContext: Show[Context] = Show.show(c =>
    c.toList.map(Show[ContextElement].show).mkString(".")
  )
}

sealed abstract class ContextElement extends Serializable {
  def json: Json
  def field: Option[String]
  def index: Option[Int]
}

private case class ArrayContext(json: Json, i: Int) extends ContextElement {
  def field: Option[String] = None
  def index: Option[Int] = Some(i)
}

private case class ObjectContext(json: Json, f: String) extends ContextElement {
  def field: Option[String] = Some(f)
  def index: Option[Int] = None
}

object ContextElement {
  def arrayContext(j: Json, i: Int): ContextElement = ArrayContext(j, i)
  def objectContext(j: Json, f: String): ContextElement = ObjectContext(j, f)

  implicit val eqContextElement: Eq[ContextElement] = Eq.instance {
    case (ArrayContext(j1, i1), ArrayContext(j2, i2)) => i1 == i2 && Eq[Json].eqv(j1, j2)
    case (ObjectContext(j1, f1), ObjectContext(j2, f2)) => f1 == f2 && Eq[Json].eqv(j1, j2)
    case _ => false
  }

  implicit val showContextElement: Show[ContextElement] = Show.show {
    case ArrayContext(_, i) => s"[$i]"
    case ObjectContext(_, f) => s"{$f}"
  }
}
