package io.circe.spray

import io.circe.{ Decoder, Printer, RootEncoder }
import io.circe.jawn._
import spray.http.{ ContentTypes, HttpCharsets, HttpEntity, MediaTypes }
import spray.httpx.marshalling.Marshaller
import spray.httpx.unmarshalling.Unmarshaller

trait CirceJsonSupport {
  def printer: Printer

  implicit final def circeJsonUnmarshaller[A](implicit decoder: Decoder[A]): Unmarshaller[A] =
    Unmarshaller[A](MediaTypes.`application/json`) {
      case x: HttpEntity.NonEmpty =>
        decode[A](x.asString(defaultCharset = HttpCharsets.`UTF-8`)).valueOr(throw _)
    }

  implicit final def circeJsonMarshaller[A](implicit encoder: RootEncoder[A]): Marshaller[A] =
    Marshaller.delegate[A, String](ContentTypes.`application/json`) { value =>
      printer.pretty(encoder(value))
    }
}

trait NoSpacesCirceJsonSupport extends CirceJsonSupport {
  final def printer: Printer = Printer.noSpaces
}

final object CirceJsonSupport extends NoSpacesCirceJsonSupport
