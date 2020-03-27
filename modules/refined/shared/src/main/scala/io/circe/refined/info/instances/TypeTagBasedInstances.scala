package io.circe.refined.info.instances

import io.circe.refined.info.TypeInfo
import scala.reflect.runtime.universe.TypeTag

class TypeTagBasedInfo[A](A: TypeTag[A]) extends TypeInfo[A] {
  override val describe: String = A.tpe.toString
}

class TypeTagBasedHKInfo[F[_], A](F: TypeTag[F[Any]], A: TypeTag[A]) extends TypeInfo[F[A]] {
  override def describe: String = s"${F.tpe.toString}[${A.tpe.toString}]"
}

class TypeTagBasedHK2Info[F[_, _], A, B](F: TypeTag[F[Any, Any]], A: TypeTag[A], B: TypeTag[B])
    extends TypeInfo[F[A, B]] {
  override def describe: String = s"${F.tpe.toString}[${A.tpe.toString}, ${B.tpe.toString}]"
}
