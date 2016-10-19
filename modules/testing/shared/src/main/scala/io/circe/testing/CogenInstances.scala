package io.circe.testing

import io.circe.{ DecodingFailure, Json }
import org.scalacheck.Cogen

private[testing] trait CogenInstances {
  implicit val cogenDecodingFailure: Cogen[DecodingFailure] = Cogen((_: DecodingFailure).hashCode.toLong)
  implicit val cogenJson: Cogen[Json] = Cogen((_: Json).hashCode.toLong)
}
