package io.circe

import _root_.squants._

import _root_.squants.Quantity

import _root_.squants.experimental.formatter.implicits._
import _root_.squants.experimental.formatter.DefaultFormatter
import _root_.squants.experimental.unitgroups.information._
import _root_.squants.experimental.formatter.syntax._
import _root_.squants.information.InformationConversions._
import _root_.squants.information.{Information, Megabytes}
import _root_.squants.time.Frequency


import scala.reflect.runtime.universe._


/**
 * Provides codecs for [[https://github.com/typelevel/squants squants]] types.
 *
 *
 * @author Quentin ADAM @waxzce
 */



package object squants {



  implicit def encodeSquantsQuantity[A <: Quantity[A]]: Encoder[A] = new Encoder[A] {
    //    implicit val informationFormatter = new DefaultFormatter(MetricInformation) // TODO, be parametrizable

    final def apply(a: A): Json = Json.obj(
      //    ("readable", Json.fromString(a.inBestUnit.toString())),
      ("number", Json.fromDoubleOrString(a.value)),
      ("unit", Json.fromString(a.unit.symbol))
      //   ("inPrimaryUnit_" + Information.primaryUnit.symbol, Json.fromDoubleOrString(a.to(Information.primaryUnit).doubleValue())))
    ) //}
  }


  implicit def decodeSquantsQuantity[A <: Quantity[A]](implicit man: TypeTag[A]): Decoder[A] = new Decoder[A] {
    final def apply(c: HCursor): Decoder.Result[A] =
      for {
    //    readable <- c.downField("readable").as[String]
        number <- c.downField("number").as[Double]
        unit <- c.downField("unit").as[String]
      } yield {

        val universeMirror = runtimeMirror(getClass.getClassLoader)
        val companionMirror = universeMirror.reflectModule(typeOf[A].typeSymbol.companion.asModule)
        val i = companionMirror.instance.asInstanceOf[Dimension[A]]

        val d = i.primaryUnit.apply(number)
        d


      }
  }





}
