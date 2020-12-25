package io.circe.refined.info

import io.circe.refined.info.instances.EmptyTypeInfo

trait TypeInfo[A] extends Serializable {
  def describe: String
}

object TypeInfo {
  def apply[A](implicit A: TypeInfo[A]): A.type = A

  def empty[A]: TypeInfo[A] = EmptyTypeInfo.asInstanceOf[TypeInfo[A]]
}
