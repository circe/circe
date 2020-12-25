package io.circe.refined.info.impl

import io.circe.refined.info.TypeInfo
import io.circe.refined.info.instances.{ TypeTagBasedHK2Info, TypeTagBasedHKInfo, TypeTagBasedInfo }
import scala.reflect.runtime.universe.TypeTag

trait TypeTagImplicits extends LowPriorityTypeTagTypeInfo0

trait LowPriorityTypeTagTypeInfo0 extends LowPriorityTypeTagTypeInfo1 {
  implicit def fromTypeTagHK[F[_], A](implicit F: TypeTag[F[Any]], A: TypeTag[A]): TypeInfo[F[A]] =
    new TypeTagBasedHKInfo(F, A)

  implicit def fromTypeTagHK2[F[_, _], A, B](
    implicit F: TypeTag[F[Any, Any]],
    A: TypeTag[A],
    B: TypeTag[B]
  ): TypeInfo[F[A, B]] =
    new TypeTagBasedHK2Info(F, A, B)
}

trait LowPriorityTypeTagTypeInfo1 extends Serializable {
  implicit def fromTypeTag[A](implicit A: TypeTag[A]): TypeInfo[A] = new TypeTagBasedInfo(A)
}
