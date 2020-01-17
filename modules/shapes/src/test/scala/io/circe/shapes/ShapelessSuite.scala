package io.circe.shapes

import io.circe.{ Decoder, Encoder, Json }
import io.circe.literal._
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import shapeless.{ :+:, ::, CNil, HNil, Nat, Sized, Witness }
import shapeless.labelled.FieldType
import shapeless.record.Record
import shapeless.syntax.singleton._

class ShapelessSuite extends CirceSuite {
  checkAll("Codec[HNil]", CodecTests[HNil].codec)
  checkAll("Codec[Int :: HNil]", CodecTests[Int :: HNil].codec)
  checkAll("Codec[String :: Int :: HNil]", CodecTests[String :: Int :: HNil].codec)
  checkAll("Codec[Record.`'foo -> String, 'bar -> Int`.T]", CodecTests[Record.`'foo -> String, 'bar -> Int`.T].codec)
  checkAll(
    """Codec[Record.`"a" -> Char, "b" -> Int, "c" -> Char`.T]""",
    CodecTests[Record.`"a" -> Char, "b" -> Int, "c" -> Char`.T].codec
  )
  checkAll("Codec[Int :+: String :+: List[Char] :+: CNil]", CodecTests[String :+: Int :+: List[Char] :+: CNil].codec)
  checkAll(
    "Codec[FieldType[Witness.`'foo`.T, Int] :+: FieldType[Witness.`'bar`.T, String] :+: CNil]",
    CodecTests[FieldType[Witness.`'foo`.T, Int] :+: FieldType[Witness.`'bar`.T, String] :+: CNil].codec
  )
  checkAll(
    """Codec[FieldType[Witness.`"a"`.T, Int] :+: FieldType[Witness.`"b"`.T, String] :+: CNil]""",
    CodecTests[FieldType[Witness.`"a"`.T, Int] :+: FieldType[Witness.`"b"`.T, String] :+: CNil].codec
  )
  checkAll("Codec[Sized[List[Int], Nat._4]]", CodecTests[Sized[List[Int], Nat._4]].codec)
  checkAll("Codec[Sized[Vector[String], Nat._10]]", CodecTests[Sized[Vector[String], Nat._10]].codec)

  val hlistDecoder = Decoder[String :: Int :: List[Char] :: HNil]

  "An hlist decoder" should "decode an array as an hlist" in forAll { (foo: String, bar: Int, baz: List[Char]) =>
    val expected = foo :: bar :: baz :: HNil
    val result = hlistDecoder.decodeJson(json"""[ $foo, $bar, $baz ]""")

    assert(result === Right(expected))
  }

  it should "accumulate errors" in forAll { (foo: String, bar: Int, baz: List[Char]) =>
    val result = hlistDecoder.decodeAccumulating(json"""[ $foo, $baz, $bar ]""".hcursor)

    assert(result.swap.exists(_.size == 2))
  }

  "The hnil decoder" should "not accept non-objects" in forAll { (j: Json) =>
    assert(Decoder[HNil].decodeJson(j).isRight == j.isObject)
  }

  val recordDecoder = Decoder[Record.`'foo -> String, 'bar -> Int`.T]

  "A record decoder" should "decode an object as a record" in forAll { (foo: String, bar: Int) =>
    val expected = Symbol("foo") ->> foo :: Symbol("bar") ->> bar :: HNil
    val result = recordDecoder.decodeJson(json"""{ "foo": $foo, "bar": $bar }""")

    assert(result === Right(expected))
  }

  it should "accumulate errors" in forAll { (foo: String, bar: Int) =>
    val result = recordDecoder.decodeAccumulating(json"""{ "foo": $bar, "bar": $foo }""".hcursor)

    assert(result.swap.exists(_.size == 2))
  }

  val sizedDecoder = Decoder[Sized[List[Int], Nat._4]]

  "A Sized decoder" should "fail if given an incorrect number of elements" in forAll { (xs: List[Int]) =>
    val values = if (xs.size == 4) xs ++ xs else xs
    val result = sizedDecoder.decodeJson(Encoder[List[Int]].apply(values))

    assert(result.isLeft)
  }

  it should "accumulate errors" in forAll { (a: Int, b: String, c: Int, d: String) =>
    val notIntB = b + "z"
    val notIntD = "a" + d
    val result = sizedDecoder.decodeAccumulating(json"""[ $a, $notIntB, $c, $notIntD ]""".hcursor)

    assert(result.swap.exists(_.size == 2))
  }
}
