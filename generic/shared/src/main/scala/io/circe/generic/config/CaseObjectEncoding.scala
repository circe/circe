package io.circe.generic.config

/**
 * Specifies how case objects in ADTs should be encoded.
 */
sealed trait CaseObjectEncoding

/**
 * Specifies that case objects should be encoded as if they were case classes
 * with no members (i.e. as an empty object).
 */
trait CaseObjectObject extends CaseObjectEncoding
final object CaseObjectObject extends CaseObjectObject

/**
 * Specifies that case objects in ADTs should be encoded as a string.
 *
 * Note that a case object that is not the leaf of an ADT (or one that is
 * statically typed as singleton type) will be encoded as an empty object,
 * regardless of this option, since the sum-of-products representation used by
 * Shapeless's `Generic` does not provide the case object name except as a
 * discriminator between ADT leaves.
 */
trait CaseObjectString extends CaseObjectEncoding
final object CaseObjectString extends CaseObjectString
