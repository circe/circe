package io.circe.generic.config

/**
 * Determines whether default values for case class members should be used
 * during decoding if the JSON does not contain valid fields.
 */
sealed trait DefaultValues

/**
 * Specifies that default values should not be used.
 */
trait NoDefaultValues extends DefaultValues
final object NoDefaultValues extends NoDefaultValues

/**
 * Specifies that default values should be used.
 */
trait UseDefaultValues extends DefaultValues
final object UseDefaultValues extends UseDefaultValues
