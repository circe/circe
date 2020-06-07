package io.circe

import cats.data.OneAnd

import scala.collection.immutable.ArraySeq
import scala.collection.{ Factory, Map }
import scala.collection.mutable.Builder
import scala.reflect.ClassTag

private[circe] trait CollectionDecoders extends LowPriorityCollectionDecoders {

  /**
   * @note The resulting instance will not be serializable (in the `java.io.Serializable` sense)
   *       unless the provided [[scala.collection.Factory]] is serializable.
   * @group Collection
   */
  implicit final def decodeMapLike[K, V, M[K, V] <: Map[K, V]](implicit
    decodeK: KeyDecoder[K],
    decodeV: Decoder[V],
    factory: Factory[(K, V), M[K, V]]
  ): Decoder[M[K, V]] = new MapDecoder[K, V, M](decodeK, decodeV) {
    final protected def createBuilder(): Builder[(K, V), M[K, V]] = factory.newBuilder
  }

  /**
   * @note The resulting instance will not be serializable (in the `java.io.Serializable` sense)
   *       unless the provided [[scala.collection.Factory]] is serializable.
   * @group Collection
   */
  implicit final def decodeIterable[A, C[A] <: Iterable[A]](implicit
    decodeA: Decoder[A],
    factory: Factory[A, C[A]]
  ): Decoder[C[A]] = new SeqDecoder[A, C](decodeA) {
    final protected def createBuilder(): Builder[A, C[A]] = factory.newBuilder
  }

  /**
   * @group Collection
   */
  implicit final def decodeArray[A](implicit
    decodeA: Decoder[A],
    factory: Factory[A, Array[A]]
  ): Decoder[Array[A]] = new SeqDecoder[A, Array](decodeA) {
    final protected def createBuilder(): Builder[A, Array[A]] = factory.newBuilder
  }

  /**
   * @note The resulting instance will not be serializable (in the `java.io.Serializable` sense)
   *       unless the provided [[scala.collection.Factory]] is serializable.
   * @group Collection
   */
  implicit final def decodeOneAnd[A, C[_]](implicit
    decodeA: Decoder[A],
    factory: Factory[A, C[A]]
  ): Decoder[OneAnd[C, A]] = new NonEmptySeqDecoder[A, C, OneAnd[C, A]](decodeA) {
    final protected def createBuilder(): Builder[A, C[A]] = factory.newBuilder
    final protected val create: (A, C[A]) => OneAnd[C, A] = (h, t) => OneAnd(h, t)
  }

  /**
   * @group Collection
   */
  implicit final def decodeArraySeq[A](implicit decodeA: Decoder[A], classTag: ClassTag[A]): Decoder[ArraySeq[A]] =
    new SeqDecoder[A, ArraySeq](decodeA) {
      final protected def createBuilder(): Builder[A, ArraySeq[A]] = ArraySeq.newBuilder[A]
    }

}

trait LowPriorityCollectionDecoders {

  /**
   * @group Collection
   */
  implicit final def decodeUntaggedArraySeq[A](implicit decodeA: Decoder[A]): Decoder[ArraySeq[A]] =
    new SeqDecoder[A, ArraySeq](decodeA) {
      override protected def createBuilder(): Builder[A, ArraySeq[A]] = ArraySeq.untagged.newBuilder
    }

}
