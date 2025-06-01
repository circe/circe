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
import cats.data.NonEmptyList
import io.circe.DecodingFailure.Reason
import io.circe.DecodingFailure.Reason.CustomReason
import io.circe.DecodingFailure.Reason.MissingField
import io.circe.DecodingFailure.Reason.WrongTypeExpectation

/**
 * The base exception type for both decoding and parsing errors.
 */
sealed abstract class Error extends Exception {
  final override def fillInStackTrace(): Throwable = this
}

/**
 * A convenience exception type for aggregating one or more decoding or parsing
 * errors.
 */
final case class Errors(errors: NonEmptyList[Error]) extends Exception {
  def toList: List[Error] = errors.head :: errors.tail

  override def fillInStackTrace(): Throwable = this
}

/**
 * An exception representing a parsing failure and wrapping the exception
 * provided by the parsing library.
 */
final case class ParsingFailure(message: String, underlying: Throwable) extends Error {
  final override def getMessage: String = message
  override def toString(): String = s"ParsingFailure: $message"
}

object ParsingFailure {
  implicit final val eqParsingFailure: Eq[ParsingFailure] = Eq.instance {
    case (ParsingFailure(m1, t1), ParsingFailure(m2, t2)) => m1 == m2 && t1 == t2
  }

  implicit final val showParsingFailure: Show[ParsingFailure] = Show.fromToString
}

/**
 * An exception representing a decoding failure and (lazily) capturing the
 * decoding history resulting in the failure.
 */
sealed abstract class DecodingFailure(private val lazyReason: Eval[Reason]) extends Error {

  def this(reason: Reason) = {
    this(Eval.now(reason))
  }

  @deprecated("use a DecodingFailure.Reason", since = "0.14.3")
  def this(message: String) = {
    this(CustomReason(message))
  }

  def history: List[CursorOp]

  /**
   * The location in the JSON where the decoding failure occurred. For various
   * reasons, in 0.14.x this may not always be present.
   */
  def pathToRootString: Option[String] = None

  def message: String =
    reason match {
      case WrongTypeExpectation(expJsType, v) => s"Got value '${v.noSpaces}' with wrong type, expecting $expJsType"
      case MissingField                       => "Missing required field"
      case CustomReason(message)              => message
    }

  final override def getMessage: String = DecodingFailure.showDecodingFailure.show(this)

  final def copy(message: String = message, history: => List[CursorOp] = history): DecodingFailure =
    DecodingFailure(CustomReason(message), history)

  def withMessage(message: String): DecodingFailure =
    copy(message = message)

  def withReason(reason: Reason): DecodingFailure =
    DecodingFailure(reason, history)

  def reason: Reason =
    lazyReason.value

  /**
   * Creates compact, human readable string representations for DecodingFailure
   * Cursor history is represented as JS style selections, i.e. ".foo.bar[3]"
   */
  override final def toString: String =
    pathToRootString.fold(
      s"DecodingFailure($message, $history)"
    )(pathToRootString => s"DecodingFailure at $pathToRootString: $message")

  override final def equals(that: Any): Boolean = that match {
    case other: DecodingFailure => DecodingFailure.eqDecodingFailure.eqv(this, other)
    case _                      => false
  }
  override final def hashCode: Int = message.hashCode
}

object DecodingFailure {
  private[this] final class DecodingFailureImpl(
    lazyReason: Eval[Reason],
    pathToRoot: Option[PathToRoot],
    ops: Eval[List[CursorOp]]
  ) extends DecodingFailure(lazyReason) {
    override final def pathToRootString: Option[String] =
      if (pathToRoot == Some(PathToRoot.empty)) {
        // For backwards compatibility. We'll make this more consistent in 0.15.x
        Some("")
      } else {
        pathToRoot.orElse(PathToRoot.fromHistory(ops.value).toOption).map(_.asPathString)
      }

    override final def history: List[CursorOp] = ops.value

    override final def withMessage(message: String): DecodingFailure =
      DecodingFailureImpl(CustomReason(message), pathToRoot, ops)

    override final def withReason(reason: Reason): DecodingFailure =
      DecodingFailureImpl(Eval.now(reason), pathToRoot, ops)
  }

  private[this] object DecodingFailureImpl {
    def apply(reason: Eval[Reason], pathToRoot: Option[PathToRoot], ops: Eval[List[CursorOp]]): DecodingFailure =
      new DecodingFailureImpl(reason, pathToRoot, ops)

    def apply(reason: Reason, pathToRoot: Option[PathToRoot], ops: Eval[List[CursorOp]]): DecodingFailure =
      apply(Eval.now(reason), pathToRoot, ops)
  }

  def apply(message: String, ops: => List[CursorOp]): DecodingFailure =
    DecodingFailureImpl(CustomReason(message), None, Eval.later(ops))

  def apply(reason: Reason, ops: => List[CursorOp]): DecodingFailure =
    DecodingFailureImpl(reason, None, Eval.later(ops))

  def apply(reason: Reason, cursor: ACursor): DecodingFailure =
    DecodingFailureImpl(reason, Some(cursor.pathToRoot), Eval.later(cursor.history))

  // Private because this is a work around that needs to go away on 0.15.x.
  private[circe] def apply(
    reason: Eval[Reason],
    pathToRoot: Option[PathToRoot],
    ops: Eval[List[CursorOp]]
  ): DecodingFailure =
    DecodingFailureImpl(reason, pathToRoot, ops)

  def unapply(error: Error): Option[(String, List[CursorOp])] = error match {
    case ParsingFailure(_, _)   => None
    case other: DecodingFailure => Some((other.message, other.history))
  }

  def fromThrowable(t: Throwable, ops: => List[CursorOp]): DecodingFailure = t match {
    case (d: DecodingFailure) => d
    case other                =>
      val sw = new java.io.StringWriter
      val pw = new java.io.PrintWriter(sw)
      other.printStackTrace(pw)
      DecodingFailure(sw.toString, ops)
  }

  implicit final val eqDecodingFailure: Eq[DecodingFailure] = Eq.instance { (a, b) =>
    a.reason == b.reason && CursorOp.eqCursorOpList.eqv(a.history, b.history)
  }

  /**
   * Creates compact, human readable string representations for DecodingFailure
   * Cursor history is represented as JS style selections, i.e. ".foo.bar[3]"
   */
  implicit final val showDecodingFailure: Show[DecodingFailure] =
    Show.fromToString

  sealed abstract class Reason
  object Reason {
    case object MissingField extends Reason
    case class WrongTypeExpectation(expectedJsonFieldType: String, jsonValue: Json) extends Reason
    case class CustomReason(message: String) extends Reason
  }
}

object Error {
  implicit final val eqError: Eq[Error] = Eq.instance {
    case (pf1: ParsingFailure, pf2: ParsingFailure)   => ParsingFailure.eqParsingFailure.eqv(pf1, pf2)
    case (df1: DecodingFailure, df2: DecodingFailure) => DecodingFailure.eqDecodingFailure.eqv(df1, df2)
    case (_, _)                                       => false
  }

  implicit final val showError: Show[Error] = Show.show {
    case pf: ParsingFailure  => ParsingFailure.showParsingFailure.show(pf)
    case df: DecodingFailure => DecodingFailure.showDecodingFailure.show(df)
  }
}
