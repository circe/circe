package io.circe.akkahttp

import akka.http.scaladsl.model.ContentTypes.`application/json`
import akka.http.scaladsl.model.ContentTypes.`text/plain(UTF-8)`
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.MalformedRequestContentRejection
import akka.http.scaladsl.server.UnsupportedRequestContentTypeRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.akkahttp.JsonSupportSpec.echoRoute
import org.scalatest.FlatSpec

class JsonSupportSpec extends FlatSpec with ScalatestRouteTest {

  "JsonSupport" should "provide unmarshalling and marshalling" in {
    val validJsonContent = HttpEntity(`application/json`, """{"requestValue":"value"}""")

    Post("/path", validJsonContent) ~> echoRoute ~> check {
      assert(status === OK)
      assert(contentType === `application/json`)
      assert(responseAs[String] === """{"responseValue":"value"}""")
    }
  }

  it should "reject contents with non application/json content type" in {
    val nonJsonContent = HttpEntity(`text/plain(UTF-8)`, "anything")

    Post("/path", nonJsonContent) ~> echoRoute ~> check {
      assert(rejection === UnsupportedRequestContentTypeRejection(Set(`application/json`)))
    }
  }

  it should "reject invalid json contents" in {
    val invalidJsonContent = HttpEntity(`application/json`, "invalid json")

    Post("/path", invalidJsonContent) ~> echoRoute ~> check {
      assert(rejection.getClass === classOf[MalformedRequestContentRejection])
    }
  }
}

object JsonSupportSpec {

  case class Request(requestValue: String)
  case class Response(responseValue: String)

  implicit val requestDecoder: Decoder[Request] = Decoder.instance[Request] { h =>
    for {
      in <- h.downField("requestValue").as[String]
    } yield {
      Request(in)
    }
  }

  implicit val responseEncoder: Encoder[Response] = Encoder.instance[Response] { response =>
    Json.obj("responseValue" -> Json.fromString(response.responseValue))
  }

  import akka.http.scaladsl.server.Directives._
  import io.circe.akkahttp.JsonSupport._

  val echoRoute = path("path") {
    post {
      entity(as[Request]) { request =>
        complete(Response(request.requestValue))
      }
    }
  }
}