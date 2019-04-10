package io.circe

import _root_.squants._
import _root_.squants.Quantity
import _root_.squants.UnitOfMeasure
import _root_.squants.experimental.unitgroups.ImplicitDimensions.space._
import _root_.squants.experimental.unitgroups.ImplicitDimensions.information._
import _root_.squants.experimental.unitgroups.ImplicitDimensions.electro._
import _root_.squants.experimental.unitgroups.ImplicitDimensions.energy._
import _root_.squants.experimental.unitgroups.ImplicitDimensions.mass._
import _root_.squants.experimental.unitgroups.ImplicitDimensions.motion._
import _root_.squants.experimental.unitgroups.ImplicitDimensions.photo._
import _root_.squants.experimental.unitgroups.ImplicitDimensions.radio._
import _root_.squants.experimental.unitgroups.ImplicitDimensions.thermal._
import _root_.squants.experimental.unitgroups.ImplicitDimensions.time._
import _root_.squants.experimental.unitgroups.si.strict.implicits._
import _root_.squants.experimental.unitgroups.UnitGroup
import _root_.squants.experimental.formatter.implicits._
import _root_.squants.experimental.formatter.syntax._
import _root_.squants.experimental.formatter.Formatter

import scala.reflect.runtime.universe._
import scala.util.{Success, Failure, Try}


/**
  * Provides codecs for [[https://github.com/typelevel/squants squants]] types.
  *
  * @author Quentin ADAM @waxzce
  */


package object squants {


  /*

  I get the perhaps inspiration from http://missingfaktor.blogspot.com/2013/12/optional-implicit-trick-in-scala.html
  Will be used for get implicits when they exists
   */
  case class Perhaps[E](value: Option[E]) {
    def fold[F](ifAbsent: => F)(ifPresent: E => F): F = {
      value.fold(ifAbsent)(ifPresent)
    }
  }

  implicit def perhaps[E](implicit ev: E = null): Perhaps[E] = {
    Perhaps(Option(ev))
  }


  def getFormatter[A <: Quantity[A]](a: A)(implicit pf: Perhaps[Formatter[A]]): Option[Formatter[A]] = {
    pf.fold[Option[Formatter[A]]] {
      None
    } { implicit ev =>
      Some(ev)
    }
  }


  implicit def encodeSquantsQuantity[A <: Quantity[A]]: Encoder[A] = new Encoder[A] {

    final def apply(a: A): Json = {

      val defaultformater = getFormatter(a)
      Json.obj(
        ("readable", Json.fromString(
          defaultformater.fold(
            a.value + " " + a.unit.symbol
          )(f => {
            implicit val ff = f
            // TODO make the implicit possible to include in runtime
            a.inBestUnit.toString()
          }))),
        ("number", Json.fromDoubleOrString(a.value)),
        ("unit", Json.fromString(a.unit.symbol)),
        ("name", Json.fromString(a.dimension.name)),
      )
    }
  }


  implicit def decodeSquantsQuantity[A <: Quantity[A]](implicit man: TypeTag[A]): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Decoder.Result[A] = {
      // This is sad to use exception, but there is reflection here....
      val results = try {
        val universeMirror = runtimeMirror(getClass.getClassLoader)
        val companionMirror = universeMirror.reflectModule(typeOf[A].typeSymbol.companion.asModule)
        val dimensionCompanionObject = companionMirror.instance.asInstanceOf[Dimension[A]]

        val readableOption = c.downField("readable").as[String].toOption
          .flatMap(readable => dimensionCompanionObject.parseString(readable).toOption)
          .orElse({
            (c.downField("unit").as[String].toOption, c.downField("number").as[Double].toOption) match {
              case (Some(unit), Some(number)) => {
                dimensionCompanionObject.units.find(_.symbol.equals(unit)).map(_.apply(number))
              }
              case _ => None
            }
          })

        readableOption.fold[Try[A]](Failure(DecodingFailure("unable to parse the provided value", c.history)))(data => Success(data))

      } catch {
        case anyException: Throwable => Failure(DecodingFailure.fromThrowable(anyException, Nil))
      }

      results.fold[Either[DecodingFailure, A]](t => Left(DecodingFailure.fromThrowable(t, Nil)), a => Right(a))

    }
  }

}
