package io.circe.generic.extras

/**
 * Fully automatic configurable codec derivation.
 *
 * Importing the contents of this package object provides [[io.circe.Decoder]] and [[io.circe.Encoder]]
 * instances for case classes (if all members have instances), "incomplete" case classes, sealed
 * trait hierarchies, etc.
 */
package object auto extends AutoDerivation
