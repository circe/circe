package io.circe.generic

object config {
  trait SnakeCaseKeys

  final def snakeCase(s: String): String =
    s.replaceAll(
      "([A-Z]+)([A-Z][a-z])",
      "$1_$2"
    ).replaceAll("([a-z\\d])([A-Z])", "$1_$2").toLowerCase
}
