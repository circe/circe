package io.circe

import cats.data.Xor
import io.circe.parser.parse
import io.circe.syntax._
import io.circe.tests.CirceSuite

class DecoderSuite extends CirceSuite {
  test("prepare with identity") {
    check { (i: Int) =>
      Decoder[Int].prepare(ACursor.ok).decodeJson(i.asJson) === Xor.right(i)
    }
  }

  test("prepare with downField") {
    check { (i: Int, k: String, m: Map[String, Int]) =>
      Decoder[Int].prepare(_.downField(k)).decodeJson(m.updated(k, i).asJson) === Xor.right(i)
    }
  }

  test("emap with identity") {
    check { (i: Int) =>
      Decoder[Int].emap(Xor.right).decodeJson(i.asJson) === Xor.right(i)
    }
  }

  test("emap with increment") {
    check { (i: Int) =>
      Decoder[Int].emap(v => Xor.right(v + 1)).decodeJson(i.asJson) === Xor.right(i + 1)
    }
  }

  test("emap with possibly failing operation") {
    check { (i: Int) =>
      val decoder = Decoder[Int].emap(v => if (v % 2 == 0) Xor.right(v) else Xor.left("Odd"))
      val expected = if (i % 2 == 0) Xor.right(i) else Xor.left(DecodingFailure("Odd", Nil))

      decoder.decodeJson(i.asJson) === expected
    }
  }

  test("failWith") {
    check { (json: Json) =>
      Decoder.failWith[Int]("Bad").decodeJson(json) === Xor.left(DecodingFailure("Bad", Nil))
    }
  }

  test("Decoder[Byte] fails on out-of-range values (#83)") {
    check { (l: Long) =>
      val json = Json.long(l)
      val result = Decoder[Byte].apply(json.hcursor)

      if (l.toByte.toLong == l) result === Xor.right(l.toByte) else result.isEmpty
    }
  }

  test("Decoder[Short] fails on out-of-range values (#83)") {
    check { (l: Long) =>
      val json = Json.long(l)
      val result = Decoder[Short].apply(json.hcursor)

      if (l.toShort.toLong == l) result === Xor.right(l.toShort) else result.isEmpty
    }
  }

  test("Decoder[Int] fails on out-of-range values (#83)") {
    check { (l: Long) =>
      val json = Json.long(l)
      val result = Decoder[Int].apply(json.hcursor)

      if (l.toInt.toLong == l) result === Xor.right(l.toInt) else result.isEmpty
    }
  }

  test("Decoder[Long] fails on out-of-range values (#83)") {
    check { (i: BigInt) =>
      val json = Json.bigDecimal(BigDecimal(i))
      val result = Decoder[Long].apply(json.hcursor)

      if (BigInt(i.toLong) == i) result === Xor.right(i.toLong) else result.isEmpty
    }
  }

  test("Decoder[Byte] fails on non-whole values (#83)") {
    check { (d: BigDecimal) =>
      val json = Json.bigDecimal(d)
      val result = Decoder[Byte].apply(json.hcursor)

      d.isWhole || result.isEmpty
    }
  }

  test("Decoder[Short] fails on non-whole values (#83)") {
    check { (d: BigDecimal) =>
      val json = Json.bigDecimal(d)
      val result = Decoder[Short].apply(json.hcursor)

      d.isWhole || result.isEmpty
    }
  }

  test("Decoder[Int] fails on non-whole values (#83)") {
    check { (d: BigDecimal) =>
      val json = Json.bigDecimal(d)
      val result = Decoder[Int].apply(json.hcursor)

      d.isWhole || result.isEmpty
    }
  }

  test("Decoder[Long] fails on non-whole values (#83)") {
    check { (d: BigDecimal) =>
      val json = Json.bigDecimal(d)
      val result = Decoder[Long].apply(json.hcursor)

      d.isWhole || result.isEmpty
    }
  }

  test("Decoder[Byte] succeeds on whole decimal values (#83)") {
    check { (v: Byte, n: Byte) =>
      val zeros = "0" * (math.abs(n.toInt) + 1)
      val Xor.Right(json) = parse(s"$v.$zeros")

      Decoder[Byte].apply(json.hcursor) === Xor.right(v)
    }
  }

  test("Decoder[Short] succeeds on whole decimal values (#83)") {
    check { (v: Short, n: Byte) =>
      val zeros = "0" * (math.abs(n.toInt) + 1)
      val Xor.Right(json) = parse(s"$v.$zeros")

      Decoder[Short].apply(json.hcursor) === Xor.right(v)
    }
  }

  test("Decoder[Int] succeeds on whole decimal values (#83)") {
    check { (v: Int, n: Byte) =>
      val zeros = "0" * (math.abs(n.toInt) + 1)
      val Xor.Right(json) = parse(s"$v.$zeros")

      Decoder[Int].apply(json.hcursor) === Xor.right(v)
    }
  }

  test("Decoder[Long] succeeds on whole decimal values (#83)") {
    check { (v: Long, n: Byte) =>
      val zeros = "0" * (math.abs(n.toInt) + 1)
      val Xor.Right(json) = parse(s"$v.$zeros")

      Decoder[Long].apply(json.hcursor) === Xor.right(v)
    }
  }

  test("Decoder[BigInt] succeeds on whole decimal values (#83)") {
    check { (v: BigInt, n: Byte) =>
      val zeros = "0" * (math.abs(n.toInt) + 1)
      val Xor.Right(json) = parse(s"$v.$zeros")

      Decoder[BigInt].apply(json.hcursor) === Xor.right(v)
    }
  }

  test("Decoder[Float] should attempt to parse string values as doubles (#173)") {
    check { (d: Float) =>
      val Xor.Right(json) = parse("\"" + d.toString + "\"")

      Decoder[Float].apply(json.hcursor) === Xor.right(d)
    }
  }


  test("Decoder[Double] should attempt to parse string values as doubles (#173)") {
    check { (d: Double) =>
      val Xor.Right(json) = parse("\"" + d.toString + "\"")

      Decoder[Double].apply(json.hcursor) === Xor.right(d)
    }
  }

  test("Decoder[BigInt] fails when producing a value would be intractable") {
    val Xor.Right(bigNumber) = parse("1e2147483647")

    assert(Decoder[BigInt].apply(bigNumber.hcursor).isEmpty)
  }
}
