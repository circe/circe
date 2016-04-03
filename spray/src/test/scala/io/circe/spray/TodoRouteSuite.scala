package io.circe.spray

import io.circe.generic.auto._
import io.circe.syntax._
import java.util.UUID
import org.scalatest.FunSuite
import org.scalatest.prop.Checkers
import spray.routing.Directives._
import spray.testkit.ScalatestRouteTest

case class Todo(id: UUID, title: String, completed: Boolean, order: Int)

object TodoRoute {
  import CirceJsonSupport._

  lazy val route = path("api" / "v1" / "todo") {
    post {
      entity(as[UUID => Todo]) { userData =>
        complete(userData(UUID.randomUUID()))
      }
    }
  }
}

class TodoRouteSuite extends FunSuite with ScalatestRouteTest with Checkers {
  import CirceJsonSupport._

  test("Our route should accept a partial todo and return a completed version") {  
    check { (title: String, completed: Boolean, order: Int) =>
      val fields = Map("title" -> title.asJson, "completed" -> completed.asJson, "order" -> order.asJson)

      Post("/api/v1/todo", fields) ~> TodoRoute.route ~> check {
        val todo = responseAs[Todo]

        todo.title === title && todo.completed === completed && todo.order === order
      }
    }
  }
}
