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

import scala.collection.mutable.Builder

private[circe] abstract class NonEmptySeqDecoder[A, C[_], S](decodeA: Decoder[A]) extends Decoder[S] { self =>
  protected def createBuilder(): Builder[A, C[A]]
  protected def create: (A, C[A]) => S
  private[this] final val decodeCA: Decoder[C[A]] = new SeqDecoder[A, C](decodeA) {
    protected final def createBuilder(): Builder[A, C[A]] = self.createBuilder()
  }

  final def apply(c: HCursor): Decoder.Result[S] = {
    val arr = c.downArray

    decodeA.tryDecode(arr) match {
      case Right(head) =>
        decodeCA.tryDecode(arr.delete) match {
          case Right(tail) => Right(create(head, tail))
          case l @ Left(_) => l.asInstanceOf[Decoder.Result[S]]
        }
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[S]]
    }
  }

  final override def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[S] = {
    val arr = c.downArray

    Decoder.accumulatingResultInstance.map2(
      decodeA.tryDecodeAccumulating(arr),
      decodeCA.tryDecodeAccumulating(arr.delete)
    )(create)
  }
}
