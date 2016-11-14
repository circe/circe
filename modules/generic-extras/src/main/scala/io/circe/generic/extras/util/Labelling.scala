package io.circe.generic.extras.util

import shapeless.{DefaultSymbolicLabelling, HList}
import shapeless.ops

object Labelling {

  trait AsList[T] extends Serializable {
    def apply(): List[Symbol]
  }

  object AsList {
    def apply[T](implicit asList: AsList[T]): AsList[T] = asList

    implicit def fromDefaultSymbolicLabelling[T, Labels <: HList]
    (implicit
     default: DefaultSymbolicLabelling.Aux[T, Labels],
     toList: ops.hlist.ToTraversable.Aux[Labels, List, Symbol]
    ): AsList[T] =
      new AsList[T] {
        def apply() = default().toList[Symbol]
      }
  }

}