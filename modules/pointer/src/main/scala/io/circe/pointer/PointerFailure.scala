package io.circe.pointer

import io.circe.CursorOp

case class PointerFailure(history: List[CursorOp]) extends Exception {
  final override def fillInStackTrace(): Throwable = this
}
