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

sealed abstract class NullOr[+A] extends Product with Serializable {

  def isNull: Boolean
  def isDefined: Boolean
  def toOption: Option[A]

  def fold[O](
    whenNull: => O,
    whenValue: A => O
  ): O = this match {
    case NullOr.Null     => whenNull
    case NullOr.Value(x) => whenValue(x)
  }

}

object NullOr {

  case object Null extends NullOr[Nothing] {
    def isNull: Boolean = true
    def isDefined: Boolean = true
    def toOption: Option[Nothing] = None
    def toEither: Option[Either[Unit, Nothing]] = Some(Left(()))
  }

  case class Value[A](value: A) extends NullOr[A] {
    def isNull: Boolean = false
    def isDefined: Boolean = true
    def toOption: Option[A] = Some(value)
    def toEither: Option[Either[Unit, A]] = Some(Right(value))
  }

  implicit def nullOrEq[A: Eq]: Eq[NullOr[A]] = Eq.instance { (a, b) =>
    (a, b) match {
      case (Null, Null)         => true
      case (Value(a), Value(b)) => implicitly[Eq[A]].eqv(a, b)
      case _                    => false
    }
  }

  implicit def nullOrInstances: Monad[NullOr] =
    new Monad[NullOr] {

      def pure[A](x: A): NullOr[A] = NullOr.Value(x)

      override def map[A, B](fa: NullOr[A])(f: A => B): NullOr[B] = fa match {
        case NullOr.Null     => NullOr.Null
        case NullOr.Value(x) => NullOr.Value(f(x))
      }

      def flatMap[A, B](fa: NullOr[A])(f: A => NullOr[B]): NullOr[B] = fa match {
        case NullOr.Null     => NullOr.Null
        case NullOr.Value(x) => f(x)
      }

      @tailrec
      def tailRecM[A, B](a: A)(f: A => NullOr[Either[A, B]]): NullOr[B] =
        f(a) match {
          case NullOr.Null            => NullOr.Null
          case NullOr.Value(Left(a1)) => tailRecM(a1)(f)
          case NullOr.Value(Right(b)) => NullOr.Value(b)
        }

    }

}
