package io.circe.pointer

case class PointerSyntaxError(position: Int, expected: String) extends Exception {
  final override def fillInStackTrace(): Throwable = this
  final override def getMessage(): String = s"Syntax error at ${position}; expected: $expected"
}
