package io.circe

trait Error extends Exception

case class ParsingFailure(message: String, underlying: Throwable) extends Error {
  override def getMessage: String = message
}

case class DecodingFailure(message: String, history: List[CursorOp]) extends Error {
  override def getMessage: String = message + history.mkString(",")

  def withMessage(message: String): DecodingFailure = copy(message = message)
}
