package io.circe.parser

import io.circe.Parser
import io.circe.jawn.JawnParser

case class ParserFactory(maxValueSize: Option[Int], allowDuplicateKeys: Boolean) {
  def newInstance(): Parser = new JawnParser(maxValueSize, allowDuplicateKeys)
}
