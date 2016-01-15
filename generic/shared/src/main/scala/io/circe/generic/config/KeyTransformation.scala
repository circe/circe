package io.circe.generic.config

/**
 * Represents a function that will be used to transform case class member names
 * to object keys.
 */
sealed trait KeyTransformation {
  def apply(s: String): String
}

/**
 * The identity function for object keys.
 */
trait KeyIdentity extends KeyTransformation {
  final def apply(s: String): String = s
}

final object KeyIdentity extends KeyIdentity

/**
 * A transformation that converts camel-cased names to snake case.
 */
trait SnakeCaseKeys extends KeyTransformation {
  final def apply(s: String): String = s.replaceAll(
    "([A-Z]+)([A-Z][a-z])",
    "$1_$2"
  ).replaceAll("([a-z\\d])([A-Z])", "$1_$2").toLowerCase
}

final object SnakeCaseKeys extends SnakeCaseKeys
