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

package io.circe.generic.util

import shapeless._
import shapeless.labelled.{ FieldType, field }

trait PatchWithOptions[R <: HList] {
  type Out <: HList

  def apply(r: R, o: Out): R
}

object PatchWithOptions {
  final type Aux[R <: HList, Out0 <: HList] = PatchWithOptions[R] { type Out = Out0 }

  implicit final val hnilPatchWithOptions: Aux[HNil, HNil] =
    new PatchWithOptions[HNil] {
      final type Out = HNil

      final def apply(r: HNil, o: HNil): HNil = HNil
    }

  implicit final def hconsPatchWithOptions[K <: Symbol, V, T <: HList](implicit
    tailPatch: PatchWithOptions[T]
  ): Aux[FieldType[K, V] :: T, FieldType[K, Option[V]] :: tailPatch.Out] =
    new PatchWithOptions[FieldType[K, V] :: T] {
      final type Out = FieldType[K, Option[V]] :: tailPatch.Out

      final def apply(
        r: FieldType[K, V] :: T,
        o: FieldType[K, Option[V]] :: tailPatch.Out
      ): FieldType[K, V] :: T =
        field[K](o.head.getOrElse(r.head)) :: tailPatch(r.tail, o.tail)
    }
}
