package io.circe.optics

import cats.std.list._
import cats.syntax.functor._
import cats.syntax.traverse._
import io.circe.{ Json, JsonNumber, JsonObject }
import io.circe.optics.JsonNumberOptics._
import monocle.{ Prism, Traversal }
import monocle.function.Plated

/**
 * Optics instances for [[io.circe.Json]].
 *
 * @author Sean Parsons
 * @author Travis Brown
 * @author Julien Truffaut
 */
trait JsonOptics extends CatsConversions {
  final lazy val jsonBoolean: Prism[Json, Boolean] = Prism[Json, Boolean](_.asBoolean)(Json.bool)
  final lazy val jsonBigDecimal: Prism[Json, BigDecimal] =
    jsonNumber.composePrism(jsonNumberBigDecimal)
  final lazy val jsonDouble: Prism[Json, Double] = jsonNumber.composePrism(jsonNumberDouble)
  final lazy val jsonBigInt: Prism[Json, BigInt] = jsonNumber.composePrism(jsonNumberBigInt)
  final lazy val jsonLong: Prism[Json, Long] = jsonNumber.composePrism(jsonNumberLong)
  final lazy val jsonInt: Prism[Json, Int] = jsonNumber.composePrism(jsonNumberInt)
  final lazy val jsonShort: Prism[Json, Short] = jsonNumber.composePrism(jsonNumberShort)
  final lazy val jsonByte: Prism[Json, Byte] = jsonNumber.composePrism(jsonNumberByte)
  final lazy val jsonString: Prism[Json, String] = Prism[Json, String](_.asString)(Json.string)
  final lazy val jsonNumber: Prism[Json, JsonNumber] =
    Prism[Json, JsonNumber](_.asNumber)(Json.fromJsonNumber)
  final lazy val jsonObject: Prism[Json, JsonObject] =
    Prism[Json, JsonObject](_.asObject)(Json.fromJsonObject)
  final lazy val jsonArray: Prism[Json, List[Json]] =
    Prism[Json, List[Json]](_.asArray)(Json.fromValues)

  implicit final lazy val jsonPlated: Plated[Json] = new Plated[Json] {
    val plate: Traversal[Json, Json] = new Traversal[Json, Json] {
      def modifyF[F[_]](f: Json => F[Json])(a: Json)(implicit
        FZ: scalaz.Applicative[F]
      ): F[Json] = {
        implicit val F = csApplicative(FZ)
        a.fold(
          F.pure(a),
          b => F.pure(Json.bool(b)),
          n => F.pure(Json.fromJsonNumber(n)),
          s => F.pure(Json.string(s)),
          _.traverse(f).map(Json.fromValues),
          _.traverse(f).map(Json.fromJsonObject)
        )
      }
    }
  }
}

final object JsonOptics extends JsonOptics
