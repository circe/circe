package io.circe

sealed abstract class Context extends Serializable {
  def json: Json
  def field: Option[String]
  def index: Option[Int]
}

object Context {
  def inArray(j: Json, i: Int): Context = ArrayContext(j, i)
  def inObject(j: Json, f: String): Context = ObjectContext(j, f)

  private[circe] case class ArrayContext(json: Json, i: Int) extends Context {
    def field: Option[String] = None
    def index: Option[Int] = Some(i)
  }

  private[circe] case class ObjectContext(json: Json, f: String) extends Context {
    def field: Option[String] = Some(f)
    def index: Option[Int] = None
  }
}
