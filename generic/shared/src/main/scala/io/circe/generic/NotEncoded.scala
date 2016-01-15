package io.circe.generic

import scala.language.experimental.macros

/**
 * A type class that witnesses that the implicit value for a type is not
 * "exported" via the mechanisms provided by export-hook.
 */
final class NotExported[A](final val value: A)

final object NotExported {
  implicit final def notExported[A]: NotExported[A] = macro DerivationMacros.notExportedImpl[A]
}
