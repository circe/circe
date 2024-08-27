/*
 * Copyright 2024 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe

import cats.data.{
  Chain,
  NonEmptyChain,
  NonEmptyList,
  NonEmptyMap,
  NonEmptySeq,
  NonEmptySet,
  NonEmptyStream,
  NonEmptyVector,
  Validated
}
import cats.kernel.Eq
import cats.laws.discipline.arbitrary._
import cats.syntax.contravariant._
import cats.syntax.invariant._
import cats.syntax.eq._
import io.circe.testing.CodecTests
import io.circe.tests.CirceMunitSuite
import io.circe.tests.examples.{ Foo, Wub }
import java.net.URI
import java.util.UUID
import org.scalacheck.{ Arbitrary, Gen }
import org.scalacheck.Prop._
import scala.collection.immutable.SortedMap
import scala.collection.mutable.HashMap
import scala.util.Success

trait SpecialEqForFloatAndDouble {

  /**
   * We provide a special [[cats.kernel.Eq]] instance for [[scala.Float]] that does not distinguish
   * `NaN` from itself.
   */
  val eqFloat: Eq[Float] = Eq.instance[Float] { (a, b) =>
    (a.isNaN && b.isNaN) || cats.instances.float.catsKernelStdOrderForFloat.eqv(a, b)
  }

  /**
   * We provide a special [[cats.kernel.Eq]] instance for [[scala.Double]] that does not distinguish
   * `NaN` from itself.
   */
  val eqDouble: Eq[Double] = Eq.instance[Double] { (a, b) =>
    (a.isNaN && b.isNaN) || cats.instances.double.catsKernelStdOrderForDouble.eqv(a, b)
  }
}
class AnyValCodecSuite extends CirceMunitSuite with SpecialEqForFloatAndDouble {
  checkAll("Codec[Unit]", CodecTests[Unit].codec)
  checkAll("Codec[Boolean]", CodecTests[Boolean].codec)
  checkAll("Codec[Char]", CodecTests[Char].codec)
  checkAll("Codec[Float]", CodecTests[Float].codec(implicitly, implicitly, eqFloat, implicitly, implicitly))
  checkAll("Codec[Double]", CodecTests[Double].codec(implicitly, implicitly, eqDouble, implicitly, implicitly))
  checkAll("Codec[Byte]", CodecTests[Byte].codec)
  checkAll("Codec[Short]", CodecTests[Short].codec)
  checkAll("Codec[Int]", CodecTests[Int].codec)
  checkAll("Codec[Long]", CodecTests[Long].codec)
}

class JavaBoxedCodecSuite extends CirceMunitSuite with SpecialEqForFloatAndDouble {
  import java.{ lang => jl }
  import java.{ math => jm }

  private def JavaCodecTests[ScalaPrimitive, JavaBoxed](
    wrap: ScalaPrimitive => JavaBoxed,
    unwrap: JavaBoxed => ScalaPrimitive,
    eq: Eq[JavaBoxed] = Eq.fromUniversalEquals[JavaBoxed]
  )(implicit scalaArb: Arbitrary[ScalaPrimitive], decoder: Decoder[JavaBoxed], encoder: Encoder[JavaBoxed]) =
    CodecTests[JavaBoxed].codec(Arbitrary(scalaArb.arbitrary.map(wrap)), implicitly, eq, implicitly, implicitly)

  checkAll("Codec[java.lang.Boolean]", JavaCodecTests[Boolean, jl.Boolean](jl.Boolean.valueOf, _.booleanValue()))
  checkAll("Codec[java.lang.Character]", JavaCodecTests[Char, jl.Character](jl.Character.valueOf, _.charValue()))
  checkAll(
    "Codec[java.lang.Float]",
    JavaCodecTests[Float, jl.Float](jl.Float.valueOf, _.floatValue(), eqFloat.contramap(_.floatValue()))
  )
  checkAll(
    "Codec[java.lang.Double]",
    JavaCodecTests[Double, jl.Double](jl.Double.valueOf, _.doubleValue(), eqDouble.contramap(_.doubleValue()))
  )
  checkAll("Codec[java.lang.Byte]", JavaCodecTests[Byte, jl.Byte](jl.Byte.valueOf, _.byteValue()))
  checkAll("Codec[java.lang.Short]", JavaCodecTests[Short, jl.Short](jl.Short.valueOf, _.shortValue()))
  checkAll("Codec[java.lang.Long]", JavaCodecTests[Long, jl.Long](jl.Long.valueOf, _.longValue()))
  checkAll("Codec[java.lang.Integer]", JavaCodecTests[Int, jl.Integer](jl.Integer.valueOf, _.intValue()))
  checkAll("Codec[java.math.BigDecimal]", JavaCodecTests[BigDecimal, jm.BigDecimal](_.bigDecimal, BigDecimal.apply))
  checkAll("Codec[java.math.BigInteger]", JavaCodecTests[BigInt, jm.BigInteger](_.bigInteger, BigInt.apply))
}

class StdLibCodecSuite extends CirceMunitSuite with ArrayFactoryInstance {
  implicit def eqHashMap[Long, Int]: Eq[HashMap[Long, Int]] = Eq.fromUniversalEquals

  implicit val arbitraryUUID: Arbitrary[UUID] = Arbitrary(Gen.uuid)

  checkAll("Codec[String]", CodecTests[String].codec)
  checkAll("Codec[BigInt]", CodecTests[BigInt].codec)
  checkAll("Codec[BigDecimal]", CodecTests[BigDecimal].codec)
  checkAll("Codec[UUID]", CodecTests[UUID].codec)
  checkAll("Codec[URI]", CodecTests[URI].codec)
  checkAll("Codec[Option[Int]]", CodecTests[Option[Int]].codec)
  checkAll("Codec[Some[Int]]", CodecTests[Some[Int]].codec)
  checkAll("Codec[None.type]", CodecTests[None.type].codec)
  checkAll("Codec[List[Int]]", CodecTests[List[Int]].codec)
  checkAll("Codec[Seq[Int]]", CodecTests[Seq[Int]].codec)
  checkAll("Codec[Map[String, Int]]", CodecTests[Map[String, Int]].codec)
  checkAll("Codec[Map[Symbol, Int]]", CodecTests[Map[Symbol, Int]].codec)
  checkAll("Codec[Map[UUID, Int]]", CodecTests[Map[UUID, Int]].codec)
  checkAll("Codec[Map[URI, Int]]", CodecTests[Map[URI, Int]].codec)
  checkAll("Codec[Map[Byte, Int]]", CodecTests[Map[Byte, Int]].codec)
  checkAll("Codec[Map[Short, Int]]", CodecTests[Map[Short, Int]].codec)
  checkAll("Codec[Map[Int, Int]]", CodecTests[Map[Int, Int]].codec)
  checkAll("Codec[Map[Long, Int]]", CodecTests[Map[Long, Int]].codec)
  checkAll("Codec[HashMap[Long, Int]]", CodecTests[HashMap[Long, Int]].unserializableCodec)
  checkAll("Codec[SortedMap[Long, Int]]", CodecTests[SortedMap[Long, Int]].unserializableCodec)
  checkAll("Codec[Set[Int]]", CodecTests[Set[Int]].codec)
  checkAll("Codec[Array[String]]", CodecTests[Array[String]].codec)

  property("A tuple encoder should return a JSON array")(tupleEncoderProp)
  private lazy val tupleEncoderProp = forAll { (t: (Int, String, Char)) =>
    val json = Encoder[(Int, String, Char)].apply(t)
    val target = Json.arr(Json.fromInt(t._1), Json.fromString(t._2), Encoder[Char].apply(t._3))

    assert(json === target && json.as[(Int, String, Char)] === Right(t))
  }

  property("A tuple decoder should fail if not given enough elements") {
    forAll { (i: Int, s: String) =>
      assert(Json.arr(Json.fromInt(i), Json.fromString(s)).as[(Int, String, Double)].isLeft)
    }
  }

  property("A tuple decoder should fail if given too many elements") {
    forAll { (i: Int, s: String, d: Double) =>
      assert(Json.arr(Json.fromInt(i), Json.fromString(s), Json.fromDoubleOrNull(d)).as[(Int, String)].isLeft)
    }
  }

  test("A list decoder should not stack overflow with a large number of elements") {
    val size = 10000
    val jsonArr = Json.arr(Seq.fill(size)(Json.fromInt(1)): _*)

    val maybeList = jsonArr.as[List[Int]]
    assert(maybeList.isRight)

    val Right(list) = maybeList
    assertEquals(list.length, size)
    assert(list.forall(_ == 1))
  }

  test("A list decoder should stop after first failure") {
    object Bomb {
      implicit val decodeBomb: Decoder[Bomb] = Decoder[Int].map {
        case 0 => throw new RuntimeException("You shouldn't have tried to decode this")
        case i => Bomb(i)
      }
    }

    case class Bomb(i: Int)

    val jsonArr = Json.arr(Json.fromInt(1), Json.fromString("foo"), Json.fromInt(0))
    val result = jsonArr.as[List[Bomb]]

    assert(result.isLeft)
  }

  test("None.type decoder should succeed for missing fields") {
    val n = Json.obj()
    val decoder = Decoder[None.type].at("foo")

    val result1 = decoder.decodeJson(n)
    assert(result1.isRight)
    val result2 = decoder.decodeAccumulating(HCursor.fromJson(n))
    assert(result2.isValid)
  }
}

class CatsCodecSuite extends CirceMunitSuite with StreamFactoryInstance {
  checkAll("Codec[Chain[Int]]", CodecTests[Chain[Int]].codec)
  checkAll("Codec[NonEmptyList[Int]]", CodecTests[NonEmptyList[Int]].codec)
  checkAll("Codec[NonEmptySeq[Int]]", CodecTests[NonEmptySeq[Int]].codec)
  checkAll("Codec[NonEmptyVector[Int]]", CodecTests[NonEmptyVector[Int]].codec)
  checkAll("Codec[NonEmptyStream[Int]]", CodecTests[NonEmptyStream[Int]].codec)
  checkAll("Codec[NonEmptySet[Int]]", CodecTests[NonEmptySet[Int]].codec)
  checkAll("Codec[NonEmptyMap[Int, String]]", CodecTests[NonEmptyMap[Int, String]].unserializableCodec)
  checkAll("Codec[NonEmptyChain[Int]]", CodecTests[NonEmptyChain[Int]].codec)
}

class CirceCodecSuite extends CirceMunitSuite {
  checkAll("Codec[Json]", CodecTests[Json].codec)
  checkAll("Codec[JsonObject]", CodecTests[JsonObject].codec)
  checkAll("Codec[JsonNumber]", CodecTests[JsonNumber].codec)
  checkAll("Codec[Foo]", CodecTests[Foo](Foo.decodeFoo, Foo.encodeFoo).codec)
}

class InvariantCodecSuite extends CirceMunitSuite {
  val wubCodec = Codec.from(Decoder[Long], Encoder[Long]).imap(Wub(_))(_.x)
  val wubCodecE = Codec.from(Decoder[Long], Encoder[Long]).iemap(l => Right(Wub(l)))(_.x)
  val wubCodecT = Codec.from(Decoder[Long], Encoder[Long]).iemapTry(l => Success(Wub(l)))(_.x)

  checkAll("Codec[Wub] via imap", CodecTests[Wub](wubCodec, wubCodec).codec)
  checkAll("Codec[Wub] via iemap", CodecTests[Wub](wubCodecE, wubCodecE).codec)
  checkAll("Codec[Wub] via iemapTry", CodecTests[Wub](wubCodecT, wubCodecT).codec)
}

class EitherCodecSuite extends CirceMunitSuite {
  val decoder = Decoder.decodeEither[Int, String]("L", "R")
  val encoder = Encoder.encodeEither[Int, String]("L", "R")
  val codec = Codec.codecForEither[Int, String]("L", "R")

  checkAll("Codec[Either[Int, String]]", CodecTests[Either[Int, String]](decoder, encoder).codec)
  checkAll("Codec[Either[Int, String]] via Codec", CodecTests[Either[Int, String]](codec, codec).codec)
  checkAll("Codec[Either[Int, String]] via Decoder and Codec", CodecTests[Either[Int, String]](decoder, codec).codec)
  checkAll("Codec[Either[Int, String]] via Encoder and Codec", CodecTests[Either[Int, String]](codec, encoder).codec)
}

class ValidatedCodecSuite extends CirceMunitSuite {
  val decoder = Decoder.decodeValidated[Int, String]("E", "A")
  val encoder = Encoder.encodeValidated[Int, String]("E", "A")
  val codec = Codec.codecForValidated[Int, String]("E", "A")

  checkAll("Codec[Validated[Int, String]]", CodecTests[Validated[Int, String]](decoder, encoder).codec)
  checkAll("Codec[Validated[Int, String]] via Codec", CodecTests[Validated[Int, String]](codec, codec).codec)
  checkAll(
    "Codec[Validated[Int, String]] via Decoder and Codec",
    CodecTests[Validated[Int, String]](decoder, codec).codec
  )
  checkAll(
    "Codec[Validated[Int, String]] via Encoder and Codec",
    CodecTests[Validated[Int, String]](codec, encoder).codec
  )
}

class DisjunctionCodecSuite extends CirceMunitSuite {
  import disjunctionCodecs._

  checkAll("Codec[Either[Int, String]]", CodecTests[Either[Int, String]].codec)
  checkAll("Codec[Validated[String, Int]]", CodecTests[Validated[String, Int]].codec)
}

class DecodingFailureSuite extends CirceMunitSuite {
  val n = Json.fromInt(10)
  val b = Json.True
  val s = Json.fromString("foo")
  val l = Json.arr(s)
  val o = Json.obj("foo" -> n)

  val nd = Decoder[Int]
  val bd = Decoder[Boolean]
  val sd = Decoder[String]
  val ld = Decoder[List[String]]
  val od = Decoder[Map[String, Int]]

  test("A JSON number should not be decoded as a non-numeric type") {
    assert(List(bd, sd, ld, od).forall(d => d.decodeJson(n).isLeft))
  }

  test("A JSON boolean should not be decoded as a non-boolean type") {
    assert(List(nd, sd, ld, od).forall(d => d.decodeJson(b).isLeft))
  }

  test("A JSON string should not be decoded as a non-string type") {
    assert(List(nd, bd, ld, od).forall(d => d.decodeJson(s).isLeft))
  }

  test("A JSON array should not be decoded as an inappropriate type") {
    assert(List(nd, bd, sd, od).forall(d => d.decodeJson(l).isLeft))
  }

  test("A JSON object should anot be decoded as an inappropriate type") {
    assert(List(nd, bd, sd, ld).forall(d => d.decodeJson(o).isLeft))
  }
}
