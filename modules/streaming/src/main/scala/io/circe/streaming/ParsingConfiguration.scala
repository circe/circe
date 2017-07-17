package io.circe.streaming

import jawn.AsyncParser

case class ParsingConfiguration(parseMode: AsyncParser.Mode)

object ParsingConfiguration {

  implicit val defaultParsingConfiguration: ParsingConfiguration =
    ParsingConfiguration(parseMode = AsyncParser.UnwrapArray)
}
