package io.circe
package scalajs

import scala.scalajs.js.JSON
import scala.util.control.NonFatal

/**
 * A parser based on the native JavaScript JSON parser.
 *
 * @see [[scala.scalajs.js.JSON]]
 */
object ScalaJSParser extends Parser {
  final def parse(input: String): Either[ParsingFailure, Json] = (
    try convertJsToJson(JSON.parse(input))
    catch {
      case NonFatal(exception) => Left(ParsingFailure(exception.getMessage, exception))
    }
  ) match {
    case r @ Right(_)    => r.asInstanceOf[Either[ParsingFailure, Json]]
    case Left(exception) => Left(ParsingFailure(exception.getMessage, exception))
  }
}
