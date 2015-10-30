package io.circe.optics

import cats.std.list.{ listInstance => catsListInstance }
import io.circe.{ Json, JsonObject }
import monocle.function.{ At, Each, FilterIndex, Index }
import monocle.{ Lens, Traversal }
import scalaz.{ Applicative, Traverse }
import scalaz.std.ListInstances

/**
 * Optics instances for [[io.circe.JsonObject]].
 *
 * @author Sean Parsons
 * @author Travis Brown
 */
trait JsonObjectOptics extends CatsConversions with ListInstances {
  implicit lazy val objectEach: Each[JsonObject, Json] = new Each[JsonObject, Json] {
    def each: Traversal[JsonObject, Json] = new Traversal[JsonObject, Json] {
      def modifyF[F[_]](f: Json => F[Json])(from: JsonObject)(implicit
        F: Applicative[F]
      ): F[JsonObject] = from.traverse(f)(csApplicative(F))
    }
  }

  implicit lazy val objectAt: At[JsonObject, String, Json] = new At[JsonObject, String, Json]{
    def at(field: String): Lens[JsonObject, Option[Json]] =
      Lens[JsonObject, Option[Json]](_.apply(field))(optVal =>
        obj => optVal.fold(obj - field)(value => obj + (field, value))
      )
  }

  implicit lazy val objectFilterIndex: FilterIndex[JsonObject, String, Json] =
    new FilterIndex[JsonObject, String, Json] {
      def filterIndex(p: String => Boolean) = new Traversal[JsonObject, Json] {
        def modifyF[F[_]](f: Json => F[Json])(from: JsonObject)(implicit
          F: Applicative[F]
        ): F[JsonObject] =
          F.map(
            Traverse[List].traverse(from.toList) {
              case (field, json) =>
                F.map(if (p(field)) f(json) else F.point(json))(field -> _)
            }
          )(JsonObject.from(_)(catsListInstance))
    }
  }

  implicit lazy val objectIndex: Index[JsonObject, String, Json] = Index.atIndex
}

object JsonObjectOptics extends JsonObjectOptics
