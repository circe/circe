package io.circe

/** A type-class for values of a Json-like type J, 
  * which allows building instances from smaller ones. 
  * This is a bit like an object Algebra. 
  */
trait JsonFactory[J] {

  /** Create a literal J that corresponds to the Json null value. 
    */
  def Null: J

  /** Create a literal J that corresponds to the Json boolean value 'true'.
    */
  def True: J

  /** Create a literal J that corresponds to the Json boolean value 'false'.
    */
  def False: J

  
  def fromString(str: String): J

  /**
    * Create a `J representing a JSON array from values.
    */
  def arr(values: J*): J

  /**
    * Create a `J` representing a JSON array from a collection of J objects representing Json values.
    */
  def fromValues(values: Iterable[J]): J

  
  /**
    * Create a `J` value representing a JSON boolean.
    */
  final def fromBoolean(value: Boolean): J = if (value) True else False

  /**
    * Create a `J` value representing a JSON number from an `Int`.
    */
  final def fromInt(value: Int): J

  /**
    * Create a `J representing a JSON number from a `Long`.
    */
  final def fromLong(value: Long): J

  /**
    * Try to create a `J representing a JSON number from a `Double`.
    *
    * The result is empty if the argument cannot be represented as a JSON number.
    */
  final def fromDouble(value: Double): Option[J] = if (isReal(value)) Some(JNumber(JsonDouble(value))) else None

  /**
    * Try to create a `Json` value representing a JSON number from a `Float`.
    *
    * The result is empty if the argument cannot be represented as a JSON number.
    */
  final def fromFloat(value: Float): Option[Json] = if (isReal(value)) Some(JNumber(JsonFloat(value))) else None


}