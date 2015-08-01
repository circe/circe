package io.jfc

import algebra.Eq

trait Error extends Exception

case class ParseFailure(message: String, underlying: Throwable) extends Error {
  override def getMessage(): String = message
}

case class DecodeFailure(message: String, history: CursorHistory) extends Error {
  override def getMessage(): String = message

  def withMessage(message: String): DecodeFailure = copy(message = message)
}

object ParseFailure {
  implicit val eqParseFailure: Eq[ParseFailure] = Eq.instance {
    case (ParseFailure(m1, t1), ParseFailure(m2, t2)) =>
      m1 == m2 && t1 == t2
  }
}

object DecodeFailure {
  implicit val eqDecodeFailure: Eq[DecodeFailure] = Eq.instance {
    case (DecodeFailure(m1, h1), DecodeFailure(m2, h2)) =>
      m1 == m2 && Eq[CursorHistory].eqv(h1, h2)
  }
}

object Error {
  implicit val eqError: Eq[Error] = Eq.instance {
    case (ParseFailure(m1, u1), ParseFailure(m2, u2)) =>
      m1 == m2 && u1 == u2
    case (DecodeFailure(m1, h1), DecodeFailure(m2, h2)) =>
      m1 == m2 && Eq[CursorHistory].eqv(h1, h2)
    case (_, _) => false
  }
}
