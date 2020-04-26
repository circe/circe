package io.circe

private[circe] trait EnumerationEncoders {

  /**
   * {{{
   *   object WeekDay extends Enumeration { ... }
   *   implicit val weekDayEncoder = Encoder.encodeEnumeration(WeekDay)
   * }}}
   * @group Utilities
   */
  final def encodeEnumeration[E <: Enumeration, J: JsonFactory](enumeration: E): Encoder[E#Value, J] = new Encoder[E#Value, J] {
    override def apply(e: E#Value): J = Encoder.encodeString.apply(e.toString)
  }

}
