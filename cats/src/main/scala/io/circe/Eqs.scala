package io.circe

import algebra.Eq
import cats.std.list._
import cats.std.map._

import io.circe.Context.{ObjectContext, ArrayContext}
import io.circe.cursor.{CObject, CArray, CJson}

trait Eqs {
  implicit val eqJson: Eq[Json] = Eq.instance(_ === _)
  implicit val eqJsonObject: Eq[JsonObject] = Eq.by(_.toMap)

  implicit val eqContext: Eq[Context] = Eq.instance {
    case (ArrayContext(j1, i1), ArrayContext(j2, i2)) => i1 == i2 && Eq[Json].eqv(j1, j2)
    case (ObjectContext(j1, f1), ObjectContext(j2, f2)) => f1 == f2 && Eq[Json].eqv(j1, j2)
    case _ => false
  }

  implicit val eqCursor: Eq[Cursor] = Eq.instance {
    case (CJson(j1), CJson(j2)) => Eq[Json].eqv(j1, j2)
    case (CArray(f1, p1, _, l1, r1), CArray(f2, p2, _, l2, r2)) =>
      eqCursor.eqv(p1, p2) && Eq[List[Json]].eqv(l1, l2) &&
        Eq[Json].eqv(f1, f2) && Eq[List[Json]].eqv(r1, r2)
    case (CObject(f1, k1, p1, _, o1), CObject(f2, k2, p2, _, o2)) =>
      eqCursor.eqv(p1, p2) && Eq[JsonObject].eqv(o1, o2) && k1 == k2 && Eq[Json].eqv(f1, f2)
    case (_, _) => false
  }

  implicit val eqHCursor: Eq[HCursor] = Eq.instance {
    case (HCursor(c1, h1), HCursor(c2, h2)) =>
      Eq[Cursor].eqv(c1, c2) && h1 == h2
  }

  implicit val eqACursor: Eq[ACursor] = Eq.by(_.either)
}
