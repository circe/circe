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

}

object Nullable {

  case object Undefined extends Nullable[Nothing] {
    def isNull: Boolean = false
    def isUndefined: Boolean = true
    def isDefined: Boolean = false
    def isValue: Boolean = false
    def toOption: Option[Nothing] = None
  }

  case object Null extends Nullable[Nothing] {
    def isNull: Boolean = true
    def isUndefined: Boolean = false
    def isDefined: Boolean = true
    def isValue: Boolean = false
    def toOption: Option[Nothing] = None
  }

  case class Value[A](value: A) extends Nullable[A] {
    def isNull: Boolean = false
    def isUndefined: Boolean = false
    def isDefined: Boolean = true
    def isValue: Boolean = true
    def toOption: Option[A] = Some(value)
  }

  implicit def nullableEq[A: Eq]: Eq[Nullable[A]] = Eq.instance { (a, b) =>
    (a, b) match {
      case (Undefined, Undefined) => true
      case (Null, Null)           => true
      case (Value(a), Value(b))   => implicitly[Eq[A]].eqv(a, b)
      case _                      => false
    }
  }

  implicit def nullableInstances: Monad[Nullable] with Traverse[Nullable] =
    new Monad[Nullable] with Traverse[Nullable] {

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

      override def ap[A, B](ff: Nullable[A => B])(fa: Nullable[A]): Nullable[B] = ff match {
        case Undefined => Undefined
        case Null      => Null
        case Value(ff) =>
          fa match {
            case Undefined => Undefined
            case Null      => Null
            case Value(fa) => Value(ff(fa))
          }
      }

      def traverse[G[_]: Applicative, A, B](fa: Nullable[A])(f: A => G[B]): G[Nullable[B]] =
        fa match {
          case Nullable.Undefined => Applicative[G].pure(Nullable.Undefined)
          case Nullable.Null      => Applicative[G].pure(Nullable.Null)
          case Nullable.Value(a)  => Applicative[G].map(f(a))(Nullable.Value(_))
        }

      def foldLeft[A, B](fa: Nullable[A], b: B)(f: (B, A) => B): B =
        fa match {
          case Nullable.Undefined => b
          case Nullable.Null      => b
          case Nullable.Value(a)  => f(b, a)
        }

      def foldRight[A, B](fa: Nullable[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] =
        fa match {
          case Nullable.Undefined => lb
          case Nullable.Null      => lb
          case Nullable.Value(a)  => f(a, lb)
        }

    }

}
