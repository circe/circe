package io.circe

private[circe] trait EnumerationEncoders {

  /**
   * {{{
   *   object WeekDay extends Enumeration { ... }
   *   implicit val weekDayEncoder = Encoder.encodeEnumeration(WeekDay)
   * }}}
   * @group Utilities
   */
  final def encodeEnumeration[E <: Enumeration](enumeration: E): Encoder[E#Value] = new Encoder[E#Value] {
    override def apply(e: E#Value): Json = Encoder.encodeString(e.toString)
  }

}
