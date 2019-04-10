package io.circe

import _root_.squants._
import _root_.squants.Quantity

import _root_.squants.experimental.formatter.syntax._
import _root_.squants.experimental.formatter._
import _root_.squants.information.{Information, Megabytes}
import _root_.squants.information.InformationConversions._
import _root_.squants.experimental.unitgroups.information.{IECInformation, MetricInformation}

import scala.reflect.runtime.universe._
import scala.util.{Failure, Success, Try}

import scala.collection.Set


/**
  * Provides codecs for [[https://github.com/typelevel/squants squants]] types.
  *
  * @author Quentin ADAM @waxzce
  */


package object squants {

  private implicit def OptionalImplicit[A <: AnyRef](implicit a: A = null) = Option(a)

  def getCompagnonObject[A <: Quantity[A]](implicit man: TypeTag[A]) = {
    val universeMirror = runtimeMirror(getClass.getClassLoader)
    val companionMirror = universeMirror.reflectModule(typeOf[A].typeSymbol.companion.asModule)
    companionMirror.instance.asInstanceOf[Dimension[A]]
  }

  implicit def encodeSquantsQuantity[A <: Quantity[A]](implicit formatter: Formatter[A] = null, man: TypeTag[A]): Encoder[A] = new Encoder[A] {

    final def apply(a: A): Json = {

      Json.obj(
        ("readable", Json.fromString(
          if (formatter != null) formatter.inBestUnit(a).toString()
          else {
            // playing with reflection, prefer to catch exceptions
            val formatedString: Option[String] = try {
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
              import _root_.squants.experimental.unitgroups.information._
              import _root_.squants.experimental.unitgroups.UnitGroup
              import _root_.squants.experimental.formatter.implicits._
              import _root_.squants.experimental.formatter.Formatters.InformationMetricFormatter
              implicit val dim: Dimension[A] = getCompagnonObject[A]
              val unitGroup:UnitGroup[A] = _root_.squants.experimental.unitgroups.si.strict.implicits.mkSiUnitGroup
              val defaultformatter = new DefaultFormatter[A](unitGroup)
              Some(defaultformatter.inBestUnit(a).toString())
            } catch {
              case anyException: Throwable => None
            }
            formatedString.getOrElse(a.toString())
          })),
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
        val dimensionCompanionObject = getCompagnonObject[A]
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
