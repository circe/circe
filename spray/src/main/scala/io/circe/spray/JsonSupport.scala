package io.circe.spray

import cats.data.Validated
import io.circe.{ Errors, Printer, RootEncoder }
import io.circe.jawn._
import spray.http.{ ContentTypes, HttpCharsets, HttpEntity, MediaTypes }
import spray.httpx.marshalling.Marshaller
import spray.httpx.unmarshalling.Unmarshaller

trait JsonSupport {
  def printer: Printer

  implicit final def circeJsonMarshaller[A](implicit encoder: RootEncoder[A]): Marshaller[A] =
    Marshaller.delegate[A, String](ContentTypes.`application/json`) { value =>
      printer.pretty(encoder(value))
    }

  implicit def circeJsonUnmarshaller[A](implicit decoder: RootDecoder[A]): Unmarshaller[A]
}

trait FailFastUnmarshaller { this: JsonSupport =>
  implicit final def circeJsonUnmarshaller[A](implicit decoder: RootDecoder[A]): Unmarshaller[A] =
    Unmarshaller[A](MediaTypes.`application/json`) {
      case x: HttpEntity.NonEmpty =>
        decode[A](x.asString(defaultCharset = HttpCharsets.`UTF-8`))(decoder.underlying).valueOr(throw _)
    }
}

trait ErrorAccumulatingUnmarshaller { this: JsonSupport =>
  implicit final def circeJsonUnmarshaller[A](implicit decoder: RootDecoder[A]): Unmarshaller[A] =
    Unmarshaller[A](MediaTypes.`application/json`) {
      case x: HttpEntity.NonEmpty =>
        decodeAccumulating[A](x.asString(defaultCharset = HttpCharsets.`UTF-8`))(decoder.underlying) match {
          case Validated.Valid(result) => result
          case Validated.Invalid(errors) => throw Errors(errors)
        }
    }
}

trait NoSpacesPrinter { this: JsonSupport =>
  final def printer: Printer = Printer.noSpaces
}

final object JsonSupport extends JsonSupport with NoSpacesPrinter with FailFastUnmarshaller

final object ErrorAccumulatingJsonSupport extends JsonSupport with NoSpacesPrinter with ErrorAccumulatingUnmarshaller
