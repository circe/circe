package io.circe

sealed abstract class CursorOpElement extends Product with Serializable

object CursorOpElement {
  case object CursorOpLeft extends CursorOpElement
  case object CursorOpRight extends CursorOpElement
  case object CursorOpFirst extends CursorOpElement
  case object CursorOpLast extends CursorOpElement
  case object CursorOpUp extends CursorOpElement
  case class CursorOpLeftN(n: Int) extends CursorOpElement
  case class CursorOpRightN(n: Int) extends CursorOpElement
  case class CursorOpLeftAt(p: Json => Boolean) extends CursorOpElement
  case class CursorOpRightAt(p: Json => Boolean) extends CursorOpElement
  case class CursorOpFind(p: Json => Boolean) extends CursorOpElement
  case class CursorOpField(k: String) extends CursorOpElement
  case class CursorOpDownField(k: String) extends CursorOpElement
  case object CursorOpDownArray extends CursorOpElement
  case class CursorOpDownAt(p: Json => Boolean) extends CursorOpElement
  case class CursorOpDownN(n: Int) extends CursorOpElement
  case object CursorOpDeleteGoParent extends CursorOpElement
  case object CursorOpDeleteGoLeft extends CursorOpElement
  case object CursorOpDeleteGoRight extends CursorOpElement
  case object CursorOpDeleteGoFirst extends CursorOpElement
  case object CursorOpDeleteGoLast extends CursorOpElement
  case class CursorOpDeleteGoField(k: String) extends CursorOpElement
  case object CursorOpDeleteLefts extends CursorOpElement
  case object CursorOpDeleteRights extends CursorOpElement
  case class CursorOpSetLefts(js: List[Json]) extends CursorOpElement
  case class CursorOpSetRights(js: List[Json]) extends CursorOpElement
}
