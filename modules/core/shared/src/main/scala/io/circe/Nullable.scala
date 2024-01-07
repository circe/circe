package io.circe

sealed abstract class Nullable[+A] extends Product with Serializable {

  def isNull: Boolean
  def isUndefined: Boolean
  def toOption: Option[A]

}

object Nullable {

  case object Undefined extends Nullable[Nothing] {
    def isNull: Boolean = false
    def isUndefined: Boolean = true
    def toOption: Option[Nothing] = None
  }

  case object Null extends Nullable[Nothing] {
    def isNull: Boolean = true
    def isUndefined: Boolean = false
    def toOption: Option[Nothing] = None
  }

  case class Value[A](value: A) extends Nullable[A] {
    def isNull: Boolean = false
    def isUndefined: Boolean = false
    def toOption: Option[A] = Some(value)
  }

}
