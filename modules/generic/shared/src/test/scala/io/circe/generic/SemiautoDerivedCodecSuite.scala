package io.circe.generic

//import cats.kernel.Eq
import io.circe._
import io.circe.generic.semiauto._
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import io.circe.tests.examples._
//import org.scalacheck.{ Arbitrary, Gen }
//import shapeless.Witness, shapeless.labelled.{ FieldType, field }
//import shapeless.test.illTyped

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
  //TODO: need to make this work with only the : Codec typeclass constraint,
  //TODO: unfortunately that cannot be done without deriving a Codec from Encoder, Decoder
  //TODO: which creates an infinity of other problems to deal with. Need to think.
  implicit def codecBox[A: Encoder: Decoder]: Codec[Box[A]] = deriveCodec[Box[A]]
  implicit def codecQux[A: Encoder: Decoder]: Codec[Qux[A]] = deriveCodec

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

  checkLaws("Codec[Foo] — explicit codec requirement", CodecTests.forCodec[Foo].codec)
  checkLaws("Codec[Box[Int]] — explicit codec requirement", CodecTests.forCodec[Box[Int]].codec)

}
