package io.jfc.generic

/**
 * Fully automatic codec derivation.
 *
 * Importing the contents of this object provides [[io.jfc.Decoder]] and [[io.jfc.Encoder]]
 * instances for tuples, case classes (if all members have instances), "incomplete" case classes,
 * sealed trait hierarchies, tuples, etc.
 */
object auto
  extends BaseInstances
  with IncompleteInstances
  with TupleInstances
  with LabelledInstances
  with GenericInstances
  with HListInstances
