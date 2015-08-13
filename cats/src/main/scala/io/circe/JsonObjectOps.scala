package io.circe

import cats.Applicative
import cats.data.Kleisli
import io.circe.JsonObject.MapAndVectorJsonObject

class JsonObjectOps(val obj: JsonObject) extends AnyVal {

  /**
   * Traverse [[Json]] values.
   */
  def traverse[F[_]](f: Json => F[Json])(implicit F: Applicative[F]): F[JsonObject] =
    obj match {
      case obj0 @ MapAndVectorJsonObject(fieldMap, orderedFields) => F.map(
        orderedFields.foldLeft(F.pure(Map.empty[String, Json])) {
          case (acc, k) => F.ap(acc)(F.map(f(fieldMap(k)))(j => _.updated(k, j)))
        }
      )(mappedFields => obj0.copy(fieldMap = mappedFields))
    }

  /**
   * Return a Kleisli arrow that gets the JSON value associated with the given field.
   */
  def kleisli: Kleisli[Option, String, Json] =
    obj match {
      case obj0: MapAndVectorJsonObject => Kleisli(obj0.fieldMap.get)
    }

}
