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

import cats._

import scala.annotation.tailrec

sealed abstract class Nullable[+A] extends Product with Serializable {

  def isNull: Boolean
  def isUndefined: Boolean
  def isDefined: Boolean
  def isValue: Boolean
  def toOption: Option[A]
  def toEither: Option[Either[Unit, A]]

  def fold[O](
    whenUndefined: => O,
    whenNull: => O,
    whenValue: A => O
  ): O = this match {
    case Nullable.Undefined => whenUndefined
    case Nullable.Null      => whenNull
    case Nullable.Value(x)  => whenValue(x)
  }

}

object Nullable {

  case object Undefined extends Nullable[Nothing] {
    def isNull: Boolean = false
    def isUndefined: Boolean = true
    def isDefined: Boolean = false
    def isValue: Boolean = false
    def toOption: Option[Nothing] = None
    def toEither: Option[Either[Unit, Nothing]] = None
  }

  case object Null extends Nullable[Nothing] {
    def isNull: Boolean = true
    def isUndefined: Boolean = false
    def isDefined: Boolean = true
    def isValue: Boolean = false
    def toOption: Option[Nothing] = None
    def toEither: Option[Either[Unit, Nothing]] = Some(Left(()))
  }

  case class Value[A](value: A) extends Nullable[A] {
    def isNull: Boolean = false
    def isUndefined: Boolean = false
    def isDefined: Boolean = true
    def isValue: Boolean = true
    def toOption: Option[A] = Some(value)
    def toEither: Option[Either[Unit, A]] = Some(Right(value))
  }

  implicit def nullableEq[A: Eq]: Eq[Nullable[A]] = Eq.instance { (a, b) =>
    (a, b) match {
      case (Undefined, Undefined) => true
      case (Null, Null)           => true
      case (Value(a), Value(b))   => implicitly[Eq[A]].eqv(a, b)
      case _                      => false
    }
  }

  implicit def nullableInstances: Monad[Nullable] =
    new Monad[Nullable] {

      def pure[A](x: A): Nullable[A] = Nullable.Value(x)

      override def map[A, B](fa: Nullable[A])(f: A => B): Nullable[B] = fa match {
        case Nullable.Undefined => Nullable.Undefined
        case Nullable.Null      => Nullable.Null
        case Nullable.Value(x)  => Nullable.Value(f(x))
      }

      def flatMap[A, B](fa: Nullable[A])(f: A => Nullable[B]): Nullable[B] = fa match {
        case Nullable.Undefined => Nullable.Undefined
        case Nullable.Null      => Nullable.Null
        case Nullable.Value(x)  => f(x)
      }

      @tailrec
      def tailRecM[A, B](a: A)(f: A => Nullable[Either[A, B]]): Nullable[B] =
        f(a) match {
          case Nullable.Undefined       => Nullable.Undefined
          case Nullable.Null            => Nullable.Null
          case Nullable.Value(Left(a1)) => tailRecM(a1)(f)
          case Nullable.Value(Right(b)) => Nullable.Value(b)
        }

    }

}
