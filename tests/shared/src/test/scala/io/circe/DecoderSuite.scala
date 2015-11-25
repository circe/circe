package io.circe

import cats.data.Xor
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
}
