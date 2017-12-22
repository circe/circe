package io.circe.generic

import io.circe._
import io.circe.generic.semiauto._
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import io.circe.tests.examples._

/**
  *
  * This test suite tests only the semi-auto derivation capabilities
  * for [[io.circe.Codec]]
  *
  * @author Lorand Szakacs, lsz@lorandszakacs.com
  * @since 22 Dec 2017
  *
  */
object SemiautoDerivedCodecSuite {
  implicit def codecBox[A: Codec]: Codec[Box[A]] = deriveCodec[Box[A]]
  implicit def codecQux[A: Codec]: Codec[Qux[A]] = deriveCodec

  implicit val CodecWub: Codec[Wub] = deriveCodec
  implicit val CodecFoo: Codec[Foo] = deriveCodec
}

class SemiautoDerivedCodecSuite extends CirceSuite {
  import SemiautoDerivedCodecSuite._

  checkLaws("Codec[Tuple1[Int]]", CodecTests[Tuple1[Int]].codec)
  checkLaws("Codec[(Int, Int, Foo)]", CodecTests[(Int, Int, Foo)].codec)
  checkLaws("Codec[Box[Int]]", CodecTests[Box[Int]].codec)
  checkLaws("Codec[Qux[Int]]", CodecTests[Qux[Int]].codec)
  checkLaws("Codec[Seq[Foo]]", CodecTests[Seq[Foo]].codec)
  checkLaws("Codec[Baz]", CodecTests[Baz].codec)
  checkLaws("Codec[Foo]", CodecTests[Foo].codec)

  //if you need Codec explicitely it should be summonable, this is too a trivial test to be worth writing
  checkLaws("Codec[Foo] — explicit codec requirement", CodecTests.forCodec[Foo].codec)
  checkLaws("Codec[Box[Int]] — explicit codec requirement", CodecTests.forCodec[Box[Int]].codec)
}
