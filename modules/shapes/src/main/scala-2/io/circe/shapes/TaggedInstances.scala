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

package io.circe.shapes

import io.circe.Decoder.Result
import io.circe._
import io.circe.syntax._
import shapeless.tag
import shapeless.tag.@@

trait TaggedInstances {
  def taggedCodec[U: Decoder: Encoder, T]: Codec[U @@ T] =
    new Codec[U @@ T] {
      override def apply(c: HCursor): Result[U @@ T] =
        Predef.implicitly[Decoder[U]].apply(c).map(u => tag[T][U](u))

      override def apply(a: U @@ T): Json = (a: U).asJson
    }

  implicit def taggedStringCodec[T](implicit d: Decoder[String], e: Encoder[String]): Codec[String @@ T] =
    taggedCodec[String, T]

  implicit def taggedDoubleCodec[T](implicit d: Decoder[Double], e: Encoder[Double]): Codec[Double @@ T] =
    taggedCodec[Double, T]

  implicit def taggedFloatCodec[T](implicit d: Decoder[Float], e: Encoder[Float]): Codec[Float @@ T] =
    taggedCodec[Float, T]

  implicit def taggedLongCodec[T](implicit d: Decoder[Long], e: Encoder[Long]): Codec[Long @@ T] =
    taggedCodec[Long, T]

  implicit def taggedIntCodec[T](implicit d: Decoder[Int], e: Encoder[Int]): Codec[Int @@ T] =
    taggedCodec[Int, T]

  implicit def taggedShortCodec[T](implicit d: Decoder[Short], e: Encoder[Short]): Codec[Short @@ T] =
    taggedCodec[Short, T]

  implicit def taggedByteCodec[T](implicit d: Decoder[Byte], e: Encoder[Byte]): Codec[Byte @@ T] =
    taggedCodec[Byte, T]

  implicit def taggedBooleanCodec[T](implicit d: Decoder[Boolean], e: Encoder[Boolean]): Codec[Boolean @@ T] =
    taggedCodec[Boolean, T]

  implicit def taggedCharCodec[T](implicit d: Decoder[Char], e: Encoder[Char]): Codec[Char @@ T] =
    taggedCodec[Char, T]
}
