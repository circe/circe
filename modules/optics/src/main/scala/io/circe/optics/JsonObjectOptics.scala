package io.circe.optics

import cats.instances.list.catsStdInstancesForList
import io.circe.{ Json, JsonObject }
import monocle.{ Lens, Traversal }
import monocle.function.{ At, Each, FilterIndex, Index }
import scalaz.{ Applicative, Traverse }
import scalaz.std.ListInstances

/**
 * Optics instances for [[io.circe.JsonObject]].
 *
 * @author Sean Parsons
 * @author Travis Brown
 */
trait JsonObjectOptics extends CatsConversions with ListInstances {
  implicit final lazy val objectEach: Each[JsonObject, Json] = new Each[JsonObject, Json] {
    final def each: Traversal[JsonObject, Json] = new Traversal[JsonObject, Json] {
      final def modifyF[F[_]](f: Json => F[Json])(from: JsonObject)(implicit
        F: Applicative[F]
      ): F[JsonObject] = from.traverse(f)(csApplicative(F))
    }
  }

  implicit final lazy val objectEachKV: Each[JsonObject, (String, Json)] = new Each[JsonObject, (String, Json)] {
    final def each: Traversal[JsonObject, (String, Json)] = new Traversal[JsonObject, (String, Json)] {
      final def modifyF[F[_]](f: ((String, Json)) => F[(String, Json)])(from: JsonObject)(implicit
        F: Applicative[F]
      ): F[JsonObject] =
        F.map(from.toList.reverse.foldLeft(F.pure(List.empty[(String, Json)])) {
          case (acc, kv) => F.apply2(acc, f(kv)){case (list, elem) => elem :: list}
        })(JsonObject.from(_))
    }
  }

  implicit final lazy val objectAt: At[JsonObject, String, Option[Json]] =
    new At[JsonObject, String, Option[Json]] {
      final def at(field: String): Lens[JsonObject, Option[Json]] =
        Lens[JsonObject, Option[Json]](_.apply(field))(optVal =>
          obj => optVal.fold(obj.remove(field))(value => obj.add(field, value))
        )
    }

  implicit final lazy val objectFilterIndex: FilterIndex[JsonObject, String, Json] =
    new FilterIndex[JsonObject, String, Json] {
      final def filterIndex(p: String => Boolean) = new Traversal[JsonObject, Json] {
        final def modifyF[F[_]](f: Json => F[Json])(from: JsonObject)(implicit
          F: Applicative[F]
        ): F[JsonObject] =
          F.map(
            Traverse[List].traverse(from.toList) {
              case (field, json) =>
                F.map(if (p(field)) f(json) else F.point(json))((field, _))
            }
          )(JsonObject.from(_)(catsStdInstancesForList))
    }
  }

  implicit final lazy val objectIndex: Index[JsonObject, String, Json] = Index.fromAt
}

final object JsonObjectOptics extends JsonObjectOptics
