package io.circe.shapeless

import io.circe.Decoder
import io.circe.literal._
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import shapeless.{ ::, HNil, Nat, Sized }
import shapeless.record.Record
import shapeless.syntax.singleton._

class ShapelessSuite extends CirceSuite {
  checkLaws("Codec[HNil]", CodecTests[HNil].codec)
  checkLaws("Codec[Int :: HNil]", CodecTests[Int :: HNil].codec)
  checkLaws("Codec[String :: Int :: HNil]", CodecTests[String :: Int :: HNil].codec)
  checkLaws("Codec[Record.`'foo -> String, 'bar -> Int`.T]", CodecTests[Record.`'foo -> String, 'bar -> Int`.T].codec)
  checkLaws(
    """Codec[Record.`"a" -> Char, "b" -> Int, "c" -> Char`.T]""",
    CodecTests[Record.`"a" -> Char, "b" -> Int, "c" -> Char`.T].codec
  )
  checkLaws("Codec[Sized[List[Int], Nat._4]]", CodecTests[Sized[List[Int], Nat._4]].codec)
  checkLaws("Codec[Sized[Vector[String], Nat._10]]", CodecTests[Sized[Vector[String], Nat._10]].codec)

  val hlistDecoder = Decoder[String :: Int :: List[Char] :: HNil]

  "An hlist decoder" should "decode an array as an hlist" in forAll { (foo: String, bar: Int, baz: List[Char]) =>
    val expected = foo :: bar :: baz :: HNil
    val result = hlistDecoder.decodeJson(json"""[ $foo, $bar, $baz ]""")

    assert(result === Right(expected))
  }

  it should "accumulated errors" in forAll { (foo: String, bar: Int, baz: List[Char]) =>
    val result = hlistDecoder.accumulating(json"""[ $foo, $baz, $bar ]""".hcursor)

    assert(result.swap.exists(_.size == 2))
  }

  val recordDecoder = Decoder[Record.`'foo -> String, 'bar -> Int`.T]

  "A record decoder" should "decode an object as a record" in forAll { (foo: String, bar: Int) =>
    val expected = 'foo ->> foo :: 'bar ->> bar :: HNil
    val result = recordDecoder.decodeJson(json"""{ "foo": $foo, "bar": $bar }""")

    assert(result === Right(expected))
  }

  it should "accumulated errors" in forAll { (foo: String, bar: Int) =>
    val result = recordDecoder.accumulating(json"""{ "foo": $bar, "bar": $foo }""".hcursor)

    assert(result.swap.exists(_.size == 2))
  }
}
