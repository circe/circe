package io.circe

import cats.data.{ NonEmptyList, NonEmptyStream, NonEmptyVector, Validated }
import cats.kernel.Eq
import cats.laws.discipline.arbitrary._
import io.circe.testing.CodecTests
import io.circe.tests.CirceSuite
import io.circe.tests.examples.Foo
import java.util.UUID
import org.scalacheck.{ Arbitrary, Gen }
import scala.collection.generic.CanBuildFrom
import scala.collection.immutable.SortedMap
import scala.collection.mutable.{ ArrayBuilder, Builder, HashMap }
import scala.reflect.ClassTag

class AnyValCodecSuite extends CirceSuite {
  /**
   * We provide a special [[cats.kernel.Eq]] instance for [[scala.Float]] that does not distinguish
   * `NaN` from itself.
   */
  val eqFloat: Eq[Float] = Eq.instance { (a, b) =>
    (a.isNaN && b.isNaN) || cats.instances.float.catsKernelStdOrderForFloat.eqv(a, b)
  }

  /**
   * We provide a special [[cats.kernel.Eq]] instance for [[scala.Double]] that does not distinguish
   * `NaN` from itself.
   */
  val eqDouble: Eq[Double] = Eq.instance { (a, b) =>
    (a.isNaN && b.isNaN) || cats.instances.double.catsKernelStdOrderForDouble.eqv(a, b)
  }

  checkLaws("Codec[Unit]", CodecTests[Unit].codec)
  checkLaws("Codec[Boolean]", CodecTests[Boolean].codec)
  checkLaws("Codec[Char]", CodecTests[Char].codec)
  checkLaws("Codec[Float]", CodecTests[Float].codec(implicitly, implicitly, eqFloat, implicitly, implicitly))
  checkLaws("Codec[Double]", CodecTests[Double].codec(implicitly, implicitly, eqDouble, implicitly, implicitly))
  checkLaws("Codec[Byte]", CodecTests[Byte].codec)
  checkLaws("Codec[Short]", CodecTests[Short].codec)
  checkLaws("Codec[Int]", CodecTests[Int].codec)
  checkLaws("Codec[Long]", CodecTests[Long].codec)
}

class JavaBoxedCodecSuite extends CirceSuite {

  private def javaLangArb[ScalaPrimitive, JavaBoxed](wrap: ScalaPrimitive => JavaBoxed)
                                                    (implicit scalaArb: Arbitrary[ScalaPrimitive]) =
    Arbitrary(scalaArb.arbitrary.map(wrap))

  implicit val arbJavaBoolean: Arbitrary[java.lang.Boolean] = javaLangArb(java.lang.Boolean.valueOf(_: Boolean))
  implicit val arbJavaByte: Arbitrary[java.lang.Byte] = javaLangArb(java.lang.Byte.valueOf(_: Byte))
  implicit val arbJavaShort: Arbitrary[java.lang.Short] = javaLangArb(java.lang.Short.valueOf(_: Short))
  implicit val arbJavaLong: Arbitrary[java.lang.Long] = javaLangArb(java.lang.Long.valueOf(_: Long))
  implicit val arbJavaInteger: Arbitrary[java.lang.Integer] = javaLangArb(java.lang.Integer.valueOf(_: Int))
  implicit val arbJavaCharacter: Arbitrary[java.lang.Character] = javaLangArb(java.lang.Character.valueOf(_: Char))

  implicit val eqJavaBoolean: Eq[java.lang.Boolean] = Eq.fromUniversalEquals
  implicit val eqJavaByte: Eq[java.lang.Byte] = Eq.fromUniversalEquals
  implicit val eqJavaShort: Eq[java.lang.Short] = Eq.fromUniversalEquals
  implicit val eqJavaLong: Eq[java.lang.Long] = Eq.fromUniversalEquals
  implicit val eqJavaInteger: Eq[java.lang.Integer] = Eq.fromUniversalEquals
  implicit val eqJavaCharacter: Eq[java.lang.Character] = Eq.fromUniversalEquals

  checkLaws("Codec[java.lang.Boolean]", CodecTests[java.lang.Boolean].codec)
  checkLaws("Codec[java.lang.Byte]", CodecTests[java.lang.Byte].codec)
  checkLaws("Codec[java.lang.Short]", CodecTests[java.lang.Short].codec)
  checkLaws("Codec[java.lang.Long]", CodecTests[java.lang.Long].codec)
  checkLaws("Codec[java.lang.Integer]", CodecTests[java.lang.Integer].codec)
  checkLaws("Codec[java.lang.Character]", CodecTests[java.lang.Character].codec)
}

class StdLibCodecSuite extends CirceSuite {
  /**
   * We need serializable `CanBuildFrom` instances for arrays for our `Array` codec tests.
   */
  implicit def canBuildFromRefArraySerializable[A <: AnyRef: ClassTag]: CanBuildFrom[Array[A], A, Array[A]] =
    new CanBuildFrom[Array[A], A, Array[A]] with Serializable {
      def apply(from: Array[A]): Builder[A, Array[A]] = new ArrayBuilder.ofRef[A]
      def apply(): Builder[A, Array[A]] = new ArrayBuilder.ofRef[A]
    }

  implicit def eqHashMap[Long, Int]: Eq[HashMap[Long, Int]] = Eq.fromUniversalEquals

  implicit val arbitraryUUID: Arbitrary[UUID] = Arbitrary(Gen.uuid)

  checkLaws("Codec[String]", CodecTests[String].codec)
  checkLaws("Codec[BigInt]", CodecTests[BigInt].codec)
  checkLaws("Codec[BigDecimal]", CodecTests[BigDecimal].codec)
  checkLaws("Codec[UUID]", CodecTests[UUID].codec)
  checkLaws("Codec[Option[Int]]", CodecTests[Option[Int]].codec)
  checkLaws("Codec[Some[Int]]", CodecTests[Some[Int]].codec)
  checkLaws("Codec[None.type]", CodecTests[None.type].codec)
  checkLaws("Codec[List[Int]]", CodecTests[List[Int]].codec)
  checkLaws("Codec[Seq[Int]]", CodecTests[Seq[Int]].codec)
  checkLaws("Codec[Map[String, Int]]", CodecTests[Map[String, Int]].codec)
  checkLaws("Codec[Map[Symbol, Int]]", CodecTests[Map[Symbol, Int]].codec)
  checkLaws("Codec[Map[UUID, Int]]", CodecTests[Map[UUID, Int]].codec)
  checkLaws("Codec[Map[Byte, Int]]", CodecTests[Map[Byte, Int]].codec)
  checkLaws("Codec[Map[Short, Int]]", CodecTests[Map[Short, Int]].codec)
  checkLaws("Codec[Map[Int, Int]]", CodecTests[Map[Int, Int]].codec)
  checkLaws("Codec[Map[Long, Int]]", CodecTests[Map[Long, Int]].codec)
  checkLaws("Codec[HashMap[Long, Int]]", CodecTests[HashMap[Long, Int]].unserializableCodec)
  checkLaws("Codec[SortedMap[Long, Int]]", CodecTests[SortedMap[Long, Int]].unserializableCodec)
  checkLaws("Codec[Set[Int]]", CodecTests[Set[Int]].codec)
  checkLaws("Codec[Array[String]]", CodecTests[Array[String]].codec)

  "A tuple encoder" should "return a JSON array" in forAll { (t: (Int, String, Char)) =>
    val json = Encoder[(Int, String, Char)].apply(t)
    val target = Json.arr(Json.fromInt(t._1), Json.fromString(t._2), Encoder[Char].apply(t._3))

    assert(json === target && json.as[(Int, String, Char)] === Right(t))
  }

  "A tuple decoder" should "fail if not given enough elements" in forAll { (i: Int, s: String) =>
    assert(Json.arr(Json.fromInt(i), Json.fromString(s)).as[(Int, String, Double)].isLeft)
  }

  it should "fail if given too many elements" in forAll { (i: Int, s: String, d: Double) =>
    assert(Json.arr(Json.fromInt(i), Json.fromString(s), Json.fromDoubleOrNull(d)).as[(Int, String)].isLeft)
  }

  "A list decoder" should "not stack overflow with a large number of elements" in {
    val size = 10000
    val jsonArr = Json.arr(Seq.fill(size)(Json.fromInt(1)): _*)

    val maybeList = jsonArr.as[List[Int]]
    assert(maybeList.isRight)

    val list = maybeList.right.getOrElse(???)
    assert(list.length == size)
    assert(list.forall(_ == 1))
  }

  it should "stop after first failure" in {
    object Bomb {
      implicit val decodeBomb: Decoder[Bomb] = Decoder[Int].map {
        case 0 => throw new Exception("You shouldn't have tried to decode this")
        case i => Bomb(i)
      }
    }

    case class Bomb(i: Int)

    val jsonArr = Json.arr(Json.fromInt(1), Json.fromString("foo"), Json.fromInt(0))
    val result = jsonArr.as[List[Bomb]]

    assert(result.isLeft)
  }
}

class CatsCodecSuite extends CirceSuite {
  /**
   * We need serializable `CanBuildFrom` instances for streams for our `NonEmptyStream` codec tests.
   */
  implicit def canBuildFromStreamSerializable[A]: CanBuildFrom[Stream[A], A, Stream[A]] =
    new CanBuildFrom[Stream[A], A, Stream[A]] with Serializable {
      def apply(from: Stream[A]): Builder[A, Stream[A]] = Stream.newBuilder[A]
      def apply(): Builder[A, Stream[A]] = Stream.newBuilder[A]
    }

  checkLaws("Codec[NonEmptyList[Int]]", CodecTests[NonEmptyList[Int]].codec)
  checkLaws("Codec[NonEmptyVector[Int]]", CodecTests[NonEmptyVector[Int]].codec)
  checkLaws("Codec[NonEmptyStream[Int]]", CodecTests[NonEmptyStream[Int]].codec)
}

class CirceCodecSuite extends CirceSuite {
  checkLaws("Codec[Json]", CodecTests[Json].codec)
  checkLaws("Codec[JsonObject]", CodecTests[JsonObject].codec)
  checkLaws("Codec[JsonNumber]", CodecTests[JsonNumber].codec)
  checkLaws("Codec[Foo]", CodecTests[Foo](Foo.decodeFoo, Foo.encodeFoo).codec)
}

class DisjunctionCodecSuite extends CirceSuite {
  import disjunctionCodecs._

  checkLaws("Codec[Either[Int, String]]", CodecTests[Either[Int, String]].codec)
  checkLaws("Codec[Validated[String, Int]]", CodecTests[Validated[String, Int]].codec)
}

class DecodingFailureSuite extends CirceSuite {
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

  "A JSON number" should "not be decoded as a non-numeric type" in {
    assert(List(bd, sd, ld, od).forall(d => d.decodeJson(n).isLeft))
  }

  "A JSON boolean" should "not be decoded as a non-boolean type" in {
    assert(List(nd, sd, ld, od).forall(d => d.decodeJson(b).isLeft))
  }

  "A JSON string" should "not be decoded as a non-string type" in {
    assert(List(nd, bd, ld, od).forall(d => d.decodeJson(s).isLeft))
  }

  "A JSON array" should "not be decoded as an inappropriate type" in {
    assert(List(nd, bd, sd, od).forall(d => d.decodeJson(l).isLeft))
  }

  "A JSON object" should "not be decoded as an inappropriate type" in {
    assert(List(nd, bd, sd, ld).forall(d => d.decodeJson(o).isLeft))
  }
}
