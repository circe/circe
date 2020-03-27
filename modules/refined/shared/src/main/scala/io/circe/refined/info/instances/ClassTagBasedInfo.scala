package io.circe.refined.info.instances

import io.circe.refined.info.TypeInfo
import scala.reflect.ClassTag

class ClassTagBasedInfo[A](A: ClassTag[A]) extends TypeInfo[A] {
  override val describe: String = A.runtimeClass.getName
}

class ClassTagBasedHKInfo[F[_], A](F: ClassTag[F[Any]], A: ClassTag[A]) extends TypeInfo[F[A]] {
  override def describe: String = s"${F.runtimeClass.getName}[${A.runtimeClass.getName}]"
}

class ClassTagBasedHK2Info[F[_, _], A, B](F: ClassTag[F[Any, Any]], A: ClassTag[A], B: ClassTag[B])
    extends TypeInfo[F[A, B]] {
  override def describe: String = s"${F.runtimeClass.getName}[${A.runtimeClass.getName}, ${B.runtimeClass.getName}]"
}
