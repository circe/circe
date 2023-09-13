/*
 * Copyright 2023 circe
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
