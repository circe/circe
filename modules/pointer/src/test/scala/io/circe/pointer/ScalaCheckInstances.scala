package io.circe.pointer

import org.scalacheck._

private[pointer] trait ScalaCheckInstances {

  /**
   * Generates a `String` which is a valid Object or Array reference according
   * to RFC 6901 for JSON pointers.
   *
   * The primary purpose of this generator is to handle escapes correctly.
   */
  final val genPointerReferenceString: Gen[String] =
    Arbitrary.arbitrary[String].map(_.replaceAll("~", "~0").replaceAll("/", "~1"))
}

object ScalaCheckInstances extends ScalaCheckInstances
