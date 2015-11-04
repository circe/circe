package io.circe

import algebra.Eq
import cats.std.list._

trait Error extends Exception

case class ParsingFailure(message: String, underlying: Throwable) extends Error {
  override def getMessage: String = message
}

case class DecodingFailure(message: String, history: List[HistoryOp]) extends Error {
  override def getMessage: String =
    if (history.isEmpty) message else s"$message: ${ history.mkString(",") }"

  def withMessage(message: String): DecodingFailure = copy(message = message)
}

object ParsingFailure {
  implicit val eqParsingFailure: Eq[ParsingFailure] = Eq.instance {
    case (ParsingFailure(m1, t1), ParsingFailure(m2, t2)) => m1 == m2 && t1 == t2
  }
}

object DecodingFailure {
  implicit val eqDecodingFailure: Eq[DecodingFailure] = Eq.instance {
    case (DecodingFailure(m1, h1), DecodingFailure(m2, h2)) =>
      m1 == m2 && Eq[List[HistoryOp]].eqv(h1, h2)
  }
}

object Error {
  implicit val eqError: Eq[Error] = Eq.instance {
    case (ParsingFailure(m1, u1), ParsingFailure(m2, u2)) =>
      m1 == m2 && u1 == u2
    case (DecodingFailure(m1, h1), DecodingFailure(m2, h2)) =>
      m1 == m2 && Eq[List[HistoryOp]].eqv(h1, h2)
    case (_, _) => false
  }
}
