package io.circe

import cats.{ Eq, Show }
import io.circe.ast.Json

sealed abstract class Context extends Serializable {
  def json: Json
  def field: Option[String]
  def index: Option[Int]
  def fold[X](field: String => X, index: Int => X): X
}

final object Context {
  final def inArray(j: Json, i: Int): Context = ArrayContext(j, i)
  final def inObject(j: Json, f: String): Context = ObjectContext(j, f)

  private[this] final case class ArrayContext(json: Json, i: Int) extends Context {
    def field: Option[String] = None
    def index: Option[Int] = Some(i)
    def fold[X](field: String => X, index: Int => X): X = index(i)
  }

  private[this] final case class ObjectContext(json: Json, f: String) extends Context {
    def field: Option[String] = Some(f)
    def index: Option[Int] = None
    def fold[X](field: String => X, index: Int => X): X = field(f)
  }

  implicit final val eqContext: Eq[Context] = Eq.instance {
    case (ArrayContext(j1, i1), ArrayContext(j2, i2)) => i1 == i2 && Eq[Json].eqv(j1, j2)
    case (ObjectContext(j1, f1), ObjectContext(j2, f2)) => f1 == f2 && Eq[Json].eqv(j1, j2)
    case _ => false
  }

  implicit final val showContext: Show[Context] = Show.show {
    case ArrayContext(_, i) => s"[$i]"
    case ObjectContext(_, f) => s"{$f}"
  }
}
