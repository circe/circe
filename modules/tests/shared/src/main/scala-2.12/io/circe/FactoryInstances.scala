package io.circe

import scala.collection.generic.CanBuildFrom
import scala.collection.mutable.{ ArrayBuilder, Builder }
import scala.reflect.ClassTag

trait ArrayFactoryInstance {

  /**
   * We need serializable `CanBuildFrom` instances for arrays for our `Array` codec tests.
   */
  implicit def canBuildFromRefArraySerializable[A <: AnyRef: ClassTag]: CanBuildFrom[Array[A], A, Array[A]] =
    new CanBuildFrom[Array[A], A, Array[A]] with Serializable {
      def apply(from: Array[A]): Builder[A, Array[A]] = new ArrayBuilder.ofRef[A]
      def apply(): Builder[A, Array[A]] = new ArrayBuilder.ofRef[A]
    }
}

trait StreamFactoryInstance {

  /**
   * We need serializable `CanBuildFrom` instances for streams for our `NonEmptyStream` codec tests.
   */
  implicit def canBuildFromStreamSerializable[A]: CanBuildFrom[Stream[A], A, Stream[A]] =
    new CanBuildFrom[Stream[A], A, Stream[A]] with Serializable {
      def apply(from: Stream[A]): Builder[A, Stream[A]] = Stream.newBuilder[A]
      def apply(): Builder[A, Stream[A]] = Stream.newBuilder[A]
    }
}
