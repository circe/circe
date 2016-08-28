# circe-akka-http

*circe-akka-http* provides support for using *circe* library with
[akka-http](http://doc.akka.io/docs/akka/current/scala/http/).

## Usage
By importing `io.circe.akkahttp.JsonSupport._` or `io.circe.akkahttp.ErrorAccumulatingJsonSupport._`
you can add circe support to your routes.

## Example

### Code

```scala
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
import io.circe.akkahttp.JsonSupport._

object Example extends App with ExampleSupport {

  implicit val actorSystem = ActorSystem("circe-akka-http")
  implicit val materializer = ActorMaterializer()

  val echoRoute = path("path") {
    post {
      entity(as[Request]) { request =>
        complete(Response(request.requestValue))
      }
    }
  }

  Http().bindAndHandle(echoRoute, "localhost", 8080)
}

trait ExampleSupport {

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
}
```

### Calling with valid json content

```bash
curl -X POST -H "Content-Type:application/json" -d "{\"requestValue\":\"value\"}" localhost:8080/path -v
```

```bash
< HTTP/1.1 200 OK
< Server: akka-http/2.4.9
< Content-Type: application/json

{"responseValue":"value"}
```

### Calling with non json content

```bash
curl -X POST -H "Content-Type:text/plain" -d "anything" localhost:8080/path -v
```

```bash
< HTTP/1.1 415 Unsupported Media Type
< Server: akka-http/2.4.9
< Content-Type: text/plain; charset=UTF-8

The request's Content-Type is not supported. Expected: application/json
```

### Calling with invalid json content

```bash
curl -X POST -H "Content-Type:application/json" -d "invalid json" localhost:8080/path -v
```

```bash
< HTTP/1.1 400 Bad Request
< Server: akka-http/2.4.9
< Content-Type: text/plain; charset=UTF-8

The request content was malformed: expected json value got i (line 1, column 1)
```