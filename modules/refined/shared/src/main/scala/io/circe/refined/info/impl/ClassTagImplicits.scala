package io.circe.refined.info.impl

import io.circe.refined.info.TypeInfo
import io.circe.refined.info.instances.{ ClassTagBasedHK2Info, ClassTagBasedHKInfo, ClassTagBasedInfo }
import scala.reflect.ClassTag

trait ClassTagImplicits extends LowPriorityClassTagTypeInfo0

trait LowPriorityClassTagTypeInfo0 extends LowPriorityClassTagTypeInfo1 {
  implicit def fromClassTagHK[F[_], A](implicit F: ClassTag[F[Any]], A: ClassTag[A]): TypeInfo[F[A]] =
    new ClassTagBasedHKInfo(F, A)

  implicit def fromClassTagHK2[F[_, _], A, B](
    implicit F: ClassTag[F[Any, Any]],
    A: ClassTag[A],
    B: ClassTag[B]
  ): TypeInfo[F[A, B]] =
    new ClassTagBasedHK2Info(F, A, B)
}

trait LowPriorityClassTagTypeInfo1 extends Serializable {
  implicit def fromClassTag[A](implicit A: ClassTag[A]): TypeInfo[A] = new ClassTagBasedInfo(A)
}
