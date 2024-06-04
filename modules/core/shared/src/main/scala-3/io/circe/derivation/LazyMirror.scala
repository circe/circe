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
package derivation

import scala.compiletime.{ constValue, constValueTuple }
import scala.deriving.Mirror

sealed trait LazyMirror[A] extends Serializable {
  lazy val mirroredLabel: String
  lazy val mirroredElemLabels: List[String]

  lazy val default: Default[A]
}

object LazyMirror {
  trait Sum[A] extends LazyMirror[A] {
    def ordinal(x: A): Int
  }
  trait Product[A] extends LazyMirror[A] {
    def fromProduct(p: scala.Product): A
  }

  final class SumImpl[A, L <: String, EL <: Tuple, ET <: Tuple](
    l: => L,
    el: => EL,
    d: => Default[A],
    ord: => A => Int
  ) extends Sum[A]
      with Serializable {
    final lazy val mirroredLabel: L = l
    final lazy val mirroredElemLabels: List[String] = el.toList.asInstanceOf[List[String]]

    final lazy val default: Default[A] = d

    final def ordinal(a: A): Int = ord(a)
  }

  final class ProductImpl[A, L <: String, EL <: Tuple, ET <: Tuple](
    l: => L,
    el: => EL,
    d: => Default[A],
    fp: => scala.Product => A
  ) extends Product[A]
      with Serializable {
    final lazy val mirroredLabel: L = l
    final lazy val mirroredElemLabels: List[String] = el.toList.asInstanceOf[List[String]]

    final lazy val default: Default[A] = d

    final def fromProduct(p: scala.Product): A = fp(p)
  }

  inline given lazyMirrorSum[A, L <: String, EL <: Tuple, ET <: Tuple](using
    inline m: Mirror.SumOf[A] {
      type MirroredLabel = L
      type MirroredElemLabels = EL
      type MirroredElemTypes = ET
    }
  ): LazyMirror.Sum[A] =
    new SumImpl[A, L, EL, ET](
      constValue[L],
      constValueTuple[EL],
      Default.mkDefault0[A, ET](constValue[Tuple.Size[EL]]),
      m.ordinal
    )

  inline given lazyMirrorProduct[A, L <: String, EL <: Tuple, ET <: Tuple](using
    inline m: Mirror.ProductOf[A] {
      type MirroredLabel = L
      type MirroredElemLabels = EL
      type MirroredElemTypes = ET
    }
  ): LazyMirror.Product[A] =
    new ProductImpl[A, L, EL, ET](
      constValue[L],
      constValueTuple[EL],
      Default.mkDefault0[A, ET](constValue[Tuple.Size[EL]]),
      m.fromProduct
    )

  private[circe] inline def fromMirror[A](inline m: Mirror.Of[A]): LazyMirror[A] =
    inline m match
      case s: Mirror.SumOf[A]     => lazyMirrorSum(using s)
      case p: Mirror.ProductOf[A] => lazyMirrorProduct(using p)
}
