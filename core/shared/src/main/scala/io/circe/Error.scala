package io.circe

import algebra.Eq
import cats.Show
import cats.std.list._
import io.circe.CursorOp._

sealed trait Error extends Exception

final case class ParsingFailure(message: String, underlying: Throwable) extends Error {
  final override def getMessage: String = message
}

final case class DecodingFailure(message: String, history: List[HistoryOp]) extends Error {
  final override def getMessage: String =
    if (history.isEmpty) message else s"$message: ${ history.mkString(",") }"

  final def withMessage(message: String): DecodingFailure = copy(message = message)
}

final object ParsingFailure {
  implicit final val eqParsingFailure: Eq[ParsingFailure] = Eq.instance {
    case (ParsingFailure(m1, t1), ParsingFailure(m2, t2)) => m1 == m2 && t1 == t2
  }

  implicit final val showParsingFailure: Show[ParsingFailure] = Show.show { failure =>
    s"ParsingFailure: ${failure.message}"
  }
}

final object DecodingFailure {
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
