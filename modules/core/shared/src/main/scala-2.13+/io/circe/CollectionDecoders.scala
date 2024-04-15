/*
 * Copyright 2024 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe

import cats.data.OneAnd

import scala.collection.Factory
import scala.collection.Map
import scala.collection.immutable.ArraySeq
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

private trait LowPriorityCollectionDecoders {

  /**
   * @group Collection
   */
  implicit final def decodeUntaggedArraySeq[A](implicit decodeA: Decoder[A]): Decoder[ArraySeq[A]] =
    new SeqDecoder[A, ArraySeq](decodeA) {
      override protected def createBuilder(): Builder[A, ArraySeq[A]] = ArraySeq.untagged.newBuilder
    }

}
