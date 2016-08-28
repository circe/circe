package io.circe.akkahttp

import akka.http.scaladsl.marshalling.Marshaller
import akka.http.scaladsl.marshalling.ToEntityMarshaller
import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import akka.http.scaladsl.unmarshalling.Unmarshaller
import akka.util.ByteString
import cats.data.Validated
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Errors
import io.circe.Printer
import io.circe.jawn.decode
import io.circe.jawn.decodeAccumulating

/**
  * Provides support for using *circe* library with *akka-http*.
  *
  * @author Tamas Polgar
  */
trait JsonSupport {
  def printer: Printer

  implicit final def circeJsonMarshaller[A](implicit encoder: Encoder[A]): ToEntityMarshaller[A] =
    Marshaller.opaque { value =>
      HttpEntity(`application/json`, ByteString(printer.pretty(encoder(value))))
    }

  implicit def circeJsonUnmarshaller[A](implicit decoder: Decoder[A]): FromEntityUnmarshaller[A]
}

trait FailFastUnmarshaller {
  this: JsonSupport =>
  implicit final def circeJsonUnmarshaller[A](implicit decoder: Decoder[A]): FromEntityUnmarshaller[A] =
    Unmarshaller.byteStringUnmarshaller.forContentTypes(`application/json`).map { byteString =>
      decode[A](byteString.utf8String).valueOr(throw _)
    }
}

trait ErrorAccumulatingUnmarshaller {
  this: JsonSupport =>
  implicit final def circeJsonUnmarshaller[A](implicit decoder: Decoder[A]): FromEntityUnmarshaller[A] =
    Unmarshaller.byteStringUnmarshaller.forContentTypes(`application/json`).map { byteString =>
      decodeAccumulating[A](byteString.utf8String) match {
        case Validated.Valid(result) => result
        case Validated.Invalid(errors) => throw Errors(errors)
      }
    }
}

trait NoSpacesPrinter {
  this: JsonSupport =>
  final def printer: Printer = Printer.noSpaces
}

object JsonSupport extends JsonSupport with NoSpacesPrinter with FailFastUnmarshaller

object ErrorAccumulatingJsonSupport extends JsonSupport with NoSpacesPrinter with ErrorAccumulatingUnmarshaller
