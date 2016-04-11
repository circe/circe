package io.circe

import algebra.Eq
import cats.Show
import cats.data.NonEmptyList
import cats.std.list._
import io.circe.CursorOp._

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

final object ParsingFailure {
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
sealed abstract class DecodingFailure(val message: String) extends Error {
  def history: List[HistoryOp]
  final override def getMessage: String =
    if (history.isEmpty) message else s"$message: ${ history.mkString(",") }"

  final def copy(message: String = message, history: => List[HistoryOp] = history): DecodingFailure = {
    def newHistory = history
    new DecodingFailure(message) {
      final lazy val history: List[HistoryOp] = newHistory
    }
  }

  final def withMessage(message: String): DecodingFailure = copy(message = message)

  override final def toString: String = s"DecodingFailure($message, $history)"
  override final def equals(that: Any): Boolean = that match {
    case other: DecodingFailure => DecodingFailure.eqDecodingFailure.eqv(this, other)
    case _ => false
  }
  override final def hashCode: Int = message.hashCode
}

final object DecodingFailure {
  def apply(message: String, ops: => List[HistoryOp]): DecodingFailure = new DecodingFailure(message) {
    final lazy val history: List[HistoryOp] = ops
  }

  def unapply(error: Error): Option[(String, List[HistoryOp])] = error match {
    case ParsingFailure(_, _) => None
    case other: DecodingFailure => Some((other.message, other.history))
  }

  implicit final val eqDecodingFailure: Eq[DecodingFailure] = Eq.instance {
    case (DecodingFailure(m1, h1), DecodingFailure(m2, h2)) =>
      m1 == m2 && Eq[List[HistoryOp]].eqv(h1, h2)
  }

  /**
    * Creates compact, human readable string representations for DecodingFailure
    * Cursor history is represented as JS style selections, i.e. ".foo.bar[3]"
    */
  implicit final val showDecodingFailure: Show[DecodingFailure] = new Show[DecodingFailure] {

    /** Represents JS style selections into JSON structure */
    private[this] sealed trait Selection
    private[this] case class SelectField(field: String) extends Selection
    private[this] case class SelectIndex(index: Int) extends Selection
    private[this] case class Op(op: CursorOp) extends Selection

    def show(failure: DecodingFailure) = {

      // Fold into sequence of selections (to reduce array ops etc. into single selections)
      val selections = failure.history.foldRight(List[Selection]()) { (historyOp, sels) =>
        (historyOp.op, sels) match {
          case (Some(DownField(k)), _)                   => SelectField(k) :: sels
          case (Some(DownArray), _)                      => SelectIndex(0) :: sels
          case (Some(MoveUp), _ :: rest)                 => rest
          case (Some(MoveRight), SelectIndex(i) :: tail) => SelectIndex(i + 1) :: tail
          case (Some(MoveLeft), SelectIndex(i) :: tail)  => SelectIndex(i - 1) :: tail
          case (Some(RightN(n)), SelectIndex(i) :: tail) => SelectIndex(i + n) :: tail
          case (Some(LeftN(n)), SelectIndex(i) :: tail)  => SelectIndex(i - n) :: tail
          case (Some(op), _)                             => Op(op) :: sels
          case (None, _)                                 => sels
        }
      }

      val selectionsStr = selections.foldLeft("") {
        case (str, SelectField(f)) => s".$f$str"
        case (str, SelectIndex(i)) => s"[$i]$str"
        case (str, Op(op))         => s"{${Show[CursorOp].show(op)}}$str"
      }

      s"DecodingFailure at $selectionsStr: ${failure.message}"
    }
  }

}

final object Error {
  implicit final val eqError: Eq[Error] = Eq.instance {
    case (pf1: ParsingFailure, pf2: ParsingFailure) => ParsingFailure.eqParsingFailure.eqv(pf1, pf2)
    case (df1: DecodingFailure, df2: DecodingFailure) =>
      DecodingFailure.eqDecodingFailure.eqv(df1, df2)
    case (_, _) => false
  }

  implicit final val showError: Show[Error] = Show.show {
    case pf: ParsingFailure  => ParsingFailure.showParsingFailure.show(pf)
    case df: DecodingFailure => DecodingFailure.showDecodingFailure.show(df)
  }
}
