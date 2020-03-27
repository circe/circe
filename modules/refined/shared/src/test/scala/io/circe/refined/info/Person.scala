package io.circe.refined.info

import eu.timepit.refined.api.Refined
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.string.MatchesRegex
import shapeless.{ Witness => W }

object Person {
  type Name = String Refined MatchesRegex[W.`"[A-z-]{2,}"`.T]
  type Age = Int Refined NonNegative
}
