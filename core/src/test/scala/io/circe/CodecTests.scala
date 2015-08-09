package io.circe

import cats.data.{ NonEmptyList, Validated, Xor }
import cats.laws.discipline.arbitrary._
import io.circe.test.{ CodecTests, JfcSuite }

class AnyValCodecTests extends JfcSuite {
  checkAll("Codec[Unit]", CodecTests[Unit].codec)
  checkAll("Codec[Boolean]", CodecTests[Boolean].codec)
  checkAll("Codec[Char]", CodecTests[Char].codec)
  checkAll("Codec[Float]", CodecTests[Float].codec)
  checkAll("Codec[Double]", CodecTests[Double].codec)
  checkAll("Codec[Byte]", CodecTests[Byte].codec)
  checkAll("Codec[Short]", CodecTests[Short].codec)
  checkAll("Codec[Int]", CodecTests[Int].codec)
  checkAll("Codec[Long]", CodecTests[Long].codec)
}

class StdLibCodecTests extends JfcSuite {
  checkAll("Codec[String]", CodecTests[String].codec)
  checkAll("Codec[BigInt]", CodecTests[BigInt].codec)
  checkAll("Codec[BigDecimal]", CodecTests[BigDecimal].codec)
  checkAll("Codec[Option[Int]]", CodecTests[Option[Int]].codec)
  checkAll("Codec[List[Int]]", CodecTests[List[Int]].codec)
  checkAll("Codec[Map[String, Int]]", CodecTests[Map[String, Int]].codec)
  checkAll("Codec[Set[Int]]", CodecTests[Set[Int]].codec)
}

class CatsCodecTests extends JfcSuite {
  checkAll("Codec[NonEmptyList[Int]]", CodecTests[NonEmptyList[Int]].codec)
}

class JfcCodecTests extends JfcSuite {
  checkAll("Codec[Json]", CodecTests[Json].codec)
}

class DisjunctionCodecTests extends JfcSuite {
  import disjunctionCodecs._

  checkAll("Codec[Xor[Int, String]]", CodecTests[Xor[Int, String]].codec)
  checkAll("Codec[Either[Int, String]]", CodecTests[Either[Int, String]].codec)
  checkAll("Codec[Validated[String, Int]]", CodecTests[Validated[String, Int]].codec)
}
