package io.circe

import cats.data.Xor
import io.circe.parser.parse
import io.circe.syntax._
import io.circe.tests.CirceSuite

class DecoderSuite extends CirceSuite with LargeNumberDecoderTests {
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

  test("Optional object field decoders fail appropriately") {
    val decoder: Decoder[Option[String]] = Decoder.instance(
      _.downField("").downField("").as[Option[String]]
    )

    check { (json: Json) =>
      val result = decoder.apply(json.hcursor)

      json.asObject match {
        // The top-level value isn't an object, so we should fail.
        case None => result.isLeft
        case Some(o1) => o1("") match {
          // The top-level object doesn't contain a "" key, so we should succeed emptily.
          case None => result === Xor.Right(None)
          case Some(j2) => j2.asObject match {
            // The second-level value isn't an object, so we should fail.
            case None => result.isLeft
            case Some(o2) => o2("") match {
              // The second-level object doesn't contain a "" key, so we should succeed emptily.
              case None => result === Xor.Right(None)
              // The third-level value is null, so we succeed emptily.
              case Some(j3) if j3.isNull => result === Xor.Right(None)
              case Some(j3) => j3.asString match {
                // The third-level value isn't a string, so we should fail.
                case None => result.isLeft
                // The third-level value is a string, so we should have decoded it.
                case Some(s3) => result === Xor.Right(Some(s3))
              }
            }
          }
        }
      }
    }
  }

  test("Optional array position decoders fail appropriately") {
    val decoder: Decoder[Option[String]] = Decoder.instance(
      _.downN(0).downN(1).as[Option[String]]
    )

    check { (json: Json) =>
      val result = decoder.apply(json.hcursor)

      json.asArray match {
        // The top-level value isn't an array, so we should fail.
        case None => result.isLeft
        case Some(a1) => a1.lift(0) match {
          // The top-level array is empty, so we should succeed emptily.
          case None => result === Xor.Right(None)
          case Some(j2) => j2.asArray match {
            // The second-level value isn't an array, so we should fail.
            case None => result.isLeft
            case Some(a2) => a2.lift(1) match {
              // The second-level array doesn't have a second element, so we should succeed emptily.
              case None => result === Xor.Right(None)
              // The third-level value is null, so we succeed emptily.
              case Some(j3) if j3.isNull => result === Xor.Right(None)
              case Some(j3) => j3.asString match {
                // The third-level value isn't a string, so we should fail.
                case None => result.isLeft
                // The third-level value is a string, so we should have decoded it.
                case Some(s3) => result === Xor.Right(Some(s3))
              }
            }
          }
        }
      }
    }
  }

  test("Decoder[Byte] fails on out-of-range values (#83)") {
    check { (l: Long) =>
      val json = Json.fromLong(l)
      val result = Decoder[Byte].apply(json.hcursor)

      if (l.toByte.toLong == l) result === Xor.right(l.toByte) else result.isEmpty
    }
  }

  test("Decoder[Short] fails on out-of-range values (#83)") {
    check { (l: Long) =>
      val json = Json.fromLong(l)
      val result = Decoder[Short].apply(json.hcursor)

      if (l.toShort.toLong == l) result === Xor.right(l.toShort) else result.isEmpty
    }
  }

  test("Decoder[Int] fails on out-of-range values (#83)") {
    check { (l: Long) =>
      val json = Json.fromLong(l)
      val result = Decoder[Int].apply(json.hcursor)

      if (l.toInt.toLong == l) result === Xor.right(l.toInt) else result.isEmpty
    }
  }

  test("Decoder[Long] fails on out-of-range values (#83)") {
    check { (i: BigInt) =>
      val json = Json.fromBigDecimal(BigDecimal(i))
      val result = Decoder[Long].apply(json.hcursor)

      if (BigInt(i.toLong) == i) result === Xor.right(i.toLong) else result.isEmpty
    }
  }

  test("Decoder[Byte] fails on non-whole values (#83)") {
    check { (d: Double) =>
      val json = Json.fromDoubleOrNull(d)
      val result = Decoder[Byte].apply(json.hcursor)

      d.isWhole || result.isEmpty
    }
  }

  test("Decoder[Short] fails on non-whole values (#83)") {
    check { (d: Double) =>
      val json = Json.fromDoubleOrNull(d)
      val result = Decoder[Short].apply(json.hcursor)

      d.isWhole || result.isEmpty
    }
  }

  test("Decoder[Int] fails on non-whole values (#83)") {
    check { (d: Double) =>
      val json = Json.fromDoubleOrNull(d)
      val result = Decoder[Int].apply(json.hcursor)

      d.isWhole || result.isEmpty
    }
  }

  test("Decoder[Long] fails on non-whole values (#83)") {
    check { (d: Double) =>
      val json = Json.fromDoubleOrNull(d)
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
