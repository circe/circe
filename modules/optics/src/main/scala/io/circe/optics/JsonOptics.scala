package io.circe.optics

import cats.instances.list._
import cats.syntax.functor._
import cats.syntax.traverse._
import io.circe.{ Json, JsonNumber, JsonObject }
import io.circe.optics.JsonNumberOptics._
import io.circe.optics.JsonObjectOptics.objectEach
import monocle.{ Prism, Traversal }
import monocle.function.{ Each, Plated }
import monocle.std.list._

/**
 * Optics instances for [[io.circe.Json]].
 *
 * @author Sean Parsons
 * @author Travis Brown
 * @author Julien Truffaut
 */
trait JsonOptics extends CatsConversions {
  final lazy val jsonNull: Prism[Json, Unit] = Prism[Json, Unit](j => if (j.isNull) Some(()) else None)(_ => Json.Null)
  final lazy val jsonBoolean: Prism[Json, Boolean] = Prism[Json, Boolean](_.asBoolean)(Json.fromBoolean)
  final lazy val jsonBigDecimal: Prism[Json, BigDecimal] = jsonNumber.composePrism(jsonNumberBigDecimal)
  final lazy val jsonBigInt: Prism[Json, BigInt] = jsonNumber.composePrism(jsonNumberBigInt)
  final lazy val jsonLong: Prism[Json, Long] = jsonNumber.composePrism(jsonNumberLong)
  final lazy val jsonInt: Prism[Json, Int] = jsonNumber.composePrism(jsonNumberInt)
  final lazy val jsonShort: Prism[Json, Short] = jsonNumber.composePrism(jsonNumberShort)
  final lazy val jsonByte: Prism[Json, Byte] = jsonNumber.composePrism(jsonNumberByte)
  final lazy val jsonString: Prism[Json, String] = Prism[Json, String](_.asString)(Json.fromString)
  final lazy val jsonNumber: Prism[Json, JsonNumber] = Prism[Json, JsonNumber](_.asNumber)(Json.fromJsonNumber)
  final lazy val jsonObject: Prism[Json, JsonObject] = Prism[Json, JsonObject](_.asObject)(Json.fromJsonObject)
  final lazy val jsonArray: Prism[Json, List[Json]] = Prism[Json, List[Json]](_.asArray)(Json.fromValues)
  final lazy val jsonDouble: Prism[Json, Double] =
    Prism[Json, Double] {
      case Json.JNull => Some(Double.NaN)
      case Json.JNumber(number) =>
        val d = number.toDouble

        if (java.lang.Double.isInfinite(d)) None else {
          if (Json.fromDouble(d).flatMap(_.asNumber).exists(JsonNumber.eqJsonNumber.eqv(number, _))) Some(d) else None
        }
      case _ => None
    }(Json.fromDoubleOrNull)

  /** points to all values of a JsonObject or JsonList */
  final lazy val jsonDescendants: Traversal[Json, Json] = new Traversal[Json, Json]{
    override def modifyF[F[_]](f: Json => F[Json])(s: Json)(implicit F: scalaz.Applicative[F]): F[Json] =
      s.fold(F.pure(s), _ => F.pure(s), _ => F.pure(s), _ => F.pure(s),
        arr => F.map(Each.each[List[Json], Json].modifyF(f)(arr))(Json.arr(_: _*)),
        obj => F.map(Each.each[JsonObject, Json].modifyF(f)(obj))(Json.fromJsonObject)
      )
  }

  implicit final lazy val jsonPlated: Plated[Json] = new Plated[Json] {
    val plate: Traversal[Json, Json] = new Traversal[Json, Json] {
      def modifyF[F[_]](f: Json => F[Json])(a: Json)(implicit
        FZ: scalaz.Applicative[F]
      ): F[Json] = {
        implicit val F = csApplicative(FZ)
        a.fold(
          F.pure(a),
          b => F.pure(Json.fromBoolean(b)),
          n => F.pure(Json.fromJsonNumber(n)),
          s => F.pure(Json.fromString(s)),
          _.traverse(f).map(Json.fromValues),
          _.traverse(f).map(Json.fromJsonObject)
        )
      }
    }
  }
}

final object JsonOptics extends JsonOptics
