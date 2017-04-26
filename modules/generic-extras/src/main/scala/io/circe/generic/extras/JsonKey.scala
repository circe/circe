package io.circe.generic.extras

import scala.annotation.StaticAnnotation

final case class JsonKey(value: String) extends StaticAnnotation
