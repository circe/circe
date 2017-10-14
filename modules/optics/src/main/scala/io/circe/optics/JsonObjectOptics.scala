package io.circe.optics

import cats.{ Applicative, Monoid, Traverse }
import cats.instances.list.catsStdInstancesForList
import io.circe.{ Json, JsonObject }
import monocle.{ Fold, Lens, Traversal }
import monocle.function.{ At, Each, FilterIndex, Index }

/**
 * Optics instances for [[io.circe.JsonObject]].
 *
 * @author Sean Parsons
 * @author Travis Brown
 */
trait JsonObjectOptics {
  final lazy val jsonObjectFields: Fold[JsonObject, (String, Json)] = new Fold[JsonObject, (String, Json)] {
    def foldMap[M: Monoid](f: ((String, Json)) => M)(obj: JsonObject): M = cats.Foldable[List].foldMap(obj.toList)(f)
  }

  implicit final lazy val jsonObjectEach: Each[JsonObject, Json] = new Each[JsonObject, Json] {
    final def each: Traversal[JsonObject, Json] = new Traversal[JsonObject, Json] {
      final def modifyF[F[_]](f: Json => F[Json])(from: JsonObject)(implicit
        F: Applicative[F]
      ): F[JsonObject] = from.traverse(f)(F)
    }
  }

  implicit final lazy val jsonObjectAt: At[JsonObject, String, Option[Json]] =
    new At[JsonObject, String, Option[Json]] {
      final def at(field: String): Lens[JsonObject, Option[Json]] =
        Lens[JsonObject, Option[Json]](_.apply(field))(optVal =>
          obj => optVal.fold(obj.remove(field))(value => obj.add(field, value))
        )
    }

  implicit final lazy val jsonObjectFilterIndex: FilterIndex[JsonObject, String, Json] =
    new FilterIndex[JsonObject, String, Json] {
      final def filterIndex(p: String => Boolean) = new Traversal[JsonObject, Json] {
        final def modifyF[F[_]](f: Json => F[Json])(from: JsonObject)(implicit
          F: Applicative[F]
        ): F[JsonObject] =
          F.map(
            Traverse[List].traverse(from.toList) {
              case (field, json) =>
                F.map(if (p(field)) f(json) else F.pure(json))((field, _))
            }
          )(JsonObject.fromFoldable(_)(catsStdInstancesForList))
    }
  }

  implicit final lazy val jsonObjectIndex: Index[JsonObject, String, Json] = Index.fromAt
}

final object JsonObjectOptics extends JsonObjectOptics
