package io.circe

import algebra.Eq
import cats.std.list._

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
}

final object DecodingFailure {
  implicit final val eqDecodingFailure: Eq[DecodingFailure] = Eq.instance {
    case (DecodingFailure(m1, h1), DecodingFailure(m2, h2)) =>
      m1 == m2 && Eq[List[HistoryOp]].eqv(h1, h2)
  }
}

final object Error {
  implicit final val eqError: Eq[Error] = Eq.instance {
    case (pf1: ParsingFailure, pf2: ParsingFailure) => ParsingFailure.eqParsingFailure.eqv(pf1, pf2)
    case (df1: DecodingFailure, df2: DecodingFailure) =>
      DecodingFailure.eqDecodingFailure.eqv(df1, df2)
    case (_, _) => false
  }
}
