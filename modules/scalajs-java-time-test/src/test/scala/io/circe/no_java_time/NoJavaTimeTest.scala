package io.circe.no_java_time

import io.circe.{ Decoder, Encoder, Json }
import munit.FunSuite

class NoJavaTimeTest extends FunSuite {
  test("Using Decoder should not throw linking errors") {
    assertEquals(Decoder[List[String]].decodeJson(Json.arr()), Right(Nil))
  }

  test("Using Encoder should not throw linking errors") {
    assertEquals(Encoder[List[String]].apply(Nil), Json.arr())
  }
}
