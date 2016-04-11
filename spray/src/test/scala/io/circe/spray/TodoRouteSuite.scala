package io.circe.spray

import akka.actor.ActorRefFactory
import io.circe.Errors
import io.circe.generic.auto._
import io.circe.syntax._
import java.util.UUID
import org.scalatest.FunSuite
import org.scalatest.prop.Checkers
import spray.http.StatusCodes.BadRequest
import spray.routing.Directives._
import spray.routing.{ HttpService, MalformedRequestContentRejection, RejectionHandler }
import spray.testkit.ScalatestRouteTest

case class Todo(id: UUID, title: String, completed: Boolean, order: Int)

object TodoRoute {
  import JsonSupport._

  lazy val route = path("api" / "v1" / "todo") {
    post {
      entity(as[UUID => Todo]) { userData =>
        complete(userData(UUID.randomUUID()))
      }
    }
  }
}

object ErrorAccumulatingTodoRoute {
  import ErrorAccumulatingJsonSupport._

  val rejectionHandler: RejectionHandler = RejectionHandler {
    case MalformedRequestContentRejection(_, Some(errors @ Errors(_))) :: _ =>
      complete((BadRequest, errors.toList.size.toString))
  }

  lazy val route = path("api" / "v1" / "todo") {
    handleRejections(rejectionHandler) {
      post {
        entity(as[UUID => Todo]) { userData =>
          complete(userData(UUID.randomUUID()))
        }
      }
    }
  }
}

class TodoRouteSuite extends FunSuite with ScalatestRouteTest with Checkers with HttpService {
  def actorRefFactory: ActorRefFactory = system

  test("Our route should accept a partial todo and return a completed version") {
    import JsonSupport._

    check { (title: String, completed: Boolean, order: Int) =>
      val fields = Map("title" -> title.asJson, "completed" -> completed.asJson, "order" -> order.asJson)

      Post("/api/v1/todo", fields) ~> TodoRoute.route ~> check {
        val todo = responseAs[Todo]

        todo.title === title && todo.completed === completed && todo.order === order
      }
    }
  }

  test("Our error-accumulating route should accept a partial todo and return a completed version") {
    import JsonSupport._

    check { (title: String, completed: Boolean, order: Int) =>
      val fields = Map("title" -> title.asJson, "completed" -> completed.asJson, "order" -> order.asJson)

      Post("/api/v1/todo", fields) ~> ErrorAccumulatingTodoRoute.route ~> check {
        val todo = responseAs[Todo]

        todo.title === title && todo.completed === completed && todo.order === order
      }
    }
  }

  test("Our error-accumulating route should fail with the proper number of errors") {
    import JsonSupport._

    check { (title: String) =>
      val fields = Map("title" -> title.asJson)

      Post("/api/v1/todo", fields) ~> ErrorAccumulatingTodoRoute.route ~> check {
        val failure = responseAs[String]

        status === BadRequest && failure === "2"
      }
    }
  }
}
