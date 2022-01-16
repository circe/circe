package io.circe

import cats.data.NonEmptyList
import cats.{ Eq, Show }
import io.circe.DecodingFailure.Reason
import io.circe.DecodingFailure.Reason.{ CustomReason, MissingField, WrongTypeExpectation }

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
}

object ParsingFailure {
  implicit final val eqParsingFailure: Eq[ParsingFailure] = Eq.instance {
    case (ParsingFailure(m1, t1), ParsingFailure(m2, t2)) => m1 == m2 && t1 == t2
  }

  implicit final val showParsingFailure: Show[ParsingFailure] = Show.show { failure =>
    s"ParsingFailure: ${failure.message}"
  }
}

/**
 * An exception representing a decoding failure and (lazily) capturing the
 * decoding history resulting in the failure.
 */
sealed abstract class DecodingFailure(val reason: Reason) extends Error {
  // Added to satisfy MiMA
  def this(message: String) = {
    this(CustomReason(message))
  }

  def history: List[CursorOp]

  val message: String = reason match {
    case WrongTypeExpectation(expJsType, v) => s"Got value '${v.noSpaces}' with wrong type, expecting $expJsType"
    case MissingField                       => "Missing required field"
    case CustomReason(message)              => message
  }

  final override def getMessage: String =
    if (history.isEmpty) message else s"$message: ${history.mkString(",")}"

  final def copy(message: String = message, history: => List[CursorOp] = history): DecodingFailure = {
    def newHistory = history
    new DecodingFailure(CustomReason(message)) {
      final lazy val history: List[CursorOp] = newHistory
    }
  }

  final def withMessage(message: String): DecodingFailure = copy(message = message)

  final def withReason(reason: Reason): DecodingFailure = {
    def newHistory: List[CursorOp] = history
    new DecodingFailure(reason) {
      override def history: List[CursorOp] = newHistory
    }
  }

  override final def toString: String = s"DecodingFailure($message, $history)"
  override final def equals(that: Any): Boolean = that match {
    case other: DecodingFailure => DecodingFailure.eqDecodingFailure.eqv(this, other)
    case _                      => false
  }
  override final def hashCode: Int = message.hashCode
}

object DecodingFailure {
  def apply(message: String, ops: => List[CursorOp]): DecodingFailure = new DecodingFailure(CustomReason(message)) {
    final lazy val history: List[CursorOp] = ops
  }

  def apply(reason: Reason, ops: => List[CursorOp]): DecodingFailure = new DecodingFailure(reason) {
    final lazy val history: List[CursorOp] = ops
  }

  def unapply(error: Error): Option[(String, List[CursorOp])] = error match {
    case ParsingFailure(_, _)   => None
    case other: DecodingFailure => Some((other.message, other.history))
  }

  def fromThrowable(t: Throwable, ops: => List[CursorOp]): DecodingFailure = t match {
    case (d: DecodingFailure) => d
    case other =>
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
  implicit final val showDecodingFailure: Show[DecodingFailure] = Show.show { failure =>
    val path = CursorOp.opsToPath(failure.history)
    s"DecodingFailure at ${path}: ${failure.message}"
  }

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
