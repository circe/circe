package io.circe

import scala.collection.{ Factory, IterableOnce }
import scala.collection.mutable.{ ArrayBuffer, ArrayBuilder, Builder }
import scala.reflect.ClassTag

trait ArrayFactoryInstance {

  /**
   * We need serializable `Factory` instances for arrays for our `Array` codec tests.
   */
  implicit def factoryRefArraySerializable[A <: AnyRef: ClassTag]: Factory[A, Array[A]] =
    new Factory[A, Array[A]] with Serializable {
      def fromSpecific(it: IterableOnce[A]): Array[A] = ArrayBuffer.from(it).toArray
      def newBuilder: Builder[A, Array[A]] = new ArrayBuilder.ofRef[A]
    }
}

trait StreamFactoryInstance {

  /**
   * We need serializable `Factory` instances for streams for our `NonEmptyStream` codec tests.
   */
  implicit def factoryStreamSerializable[A]: Factory[A, Stream[A]] =
    new Factory[A, Stream[A]] with Serializable {
      def fromSpecific(it: IterableOnce[A]): Stream[A] = Stream.from(it)
      def newBuilder: Builder[A, Stream[A]] = Stream.newBuilder[A]
    }
}
