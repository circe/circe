package todo

import akka.actor.{ Actor, ActorRef, ActorRefFactory, ActorSystem, Props }
import akka.event.Logging
import akka.io.IO
import akka.pattern.ask
import akka.util.Timeout
import cats.syntax.show._
import io.circe.{ DecodingFailure, Errors }
import io.circe.generic.auto._
import io.circe.spray.ErrorAccumulatingJsonSupport._
import java.util.UUID
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import spray.can.Http
import spray.http.StatusCodes.BadRequest
import spray.routing.Directives._
import spray.routing.{ HttpService, MalformedRequestContentRejection, RejectionHandler, Route }

case class Todo(id: UUID, title: String, completed: Boolean, order: Int)

object Boot extends App {
  implicit val system: ActorSystem = ActorSystem("todo-service")
  implicit val timeout: Timeout = Timeout(1.second)
  val service: ActorRef = system.actorOf(Props[TodoService], "todo-service")

  IO(Http).ask(Http.Bind(service, interface = "localhost", port = 8080))
    .mapTo[Http.Event]
    .map {
      case Http.CommandFailed(_) => system.shutdown()
    }
}

class TodoService extends Actor with HttpService {
  def actorRefFactory: ActorRefFactory = context

  val rejectionHandler: RejectionHandler = RejectionHandler {
    case MalformedRequestContentRejection(_, Some(errors @ Errors(_))) :: _ =>
      val errorMessages: List[String] = errors.toList.map {
        case decoding @ DecodingFailure(_, _) => decoding.show
        case other => other.getMessage
      }

      complete((BadRequest, errorMessages))
  }

  val route: Route = path("api" / "v1" / "todo") {
    handleRejections(rejectionHandler) {
      post {
        entity(as[UUID => Todo]) { userData =>
          complete(userData(UUID.randomUUID()))
        }
      }
    }
  }

  def receive: PartialFunction[Any, Unit] = runRoute(route)
}
