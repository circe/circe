package io.circe

import cats.kernel.instances.string._
import cats.syntax.all._
import io.circe.CursorOp._
import munit.ScalaCheckSuite
import org.scalacheck.{ Gen, Prop }
import org.scalacheck.Prop._

trait GenCursorOps {
  val arrayMoves: Gen[List[CursorOp]] = {

    def loop(movesRemaining: Int, leftMoves: Int, rightMoves: Int, acc: Gen[List[CursorOp]]): Gen[List[CursorOp]] =
      if (movesRemaining <= 0) {
        acc
      } else {
        if (leftMoves >= rightMoves) {
          // Can't move left again, otherwise we'll go < the 0 index.
          loop(movesRemaining - 1, leftMoves, rightMoves + 1, acc.map(value => MoveRight :: value))
        } else {
          // Can move either direction
          Gen
            .oneOf(
              MoveLeft,
              MoveRight
            )
            .flatMap {
              case MoveLeft =>
                loop(movesRemaining - 1, leftMoves + 1, rightMoves, acc.map(value => MoveLeft :: value))
              case MoveRight =>
                loop(movesRemaining - 1, leftMoves, rightMoves + 1, acc.map(value => MoveRight :: value))
            }
        }
      }

    Gen.choose(1, 100).flatMap(moves => loop(moves, 0, 0, Gen.const(List.empty)))
  }

  val downFields: Gen[List[CursorOp]] = Gen.listOf(Gen.identifier.map(DownField))
}

class ShowErrorSuite extends ScalaCheckSuite with GenCursorOps {
  import ShowErrorSuite._

  test("Show[ParsingFailure] should produce the expected output") {
    assertEquals(ParsingFailure("the message", new RuntimeException()).show, "ParsingFailure: the message")
  }

  test("Show[DecodingFailure] should produce the expected output on a small example") {
    val ops = List(MoveRight, MoveRight, DownArray, DownField("bar"), DownField("foo"))

    assertEquals(DecodingFailure("the message", ops).show, "DecodingFailure at .foo.bar[2]: the message")
  }

  test("DecodingFailure.toString should be equivalent to Show") {
    val ops = List(MoveRight, MoveRight, DownArray, DownField("bar"), DownField("foo"))

    val df = DecodingFailure("the message", ops)
    assertEquals(df.show, df.toString)
  }

  test("Show[DecodingFailure] should produce the expected output on a larger example") {
    val ops = List(
      MoveLeft,
      MoveLeft,
      MoveLeft,
      MoveRight,
      MoveRight,
      MoveRight,
      MoveRight,
      MoveRight,
      DownArray,
      DownArray,
      DownField("bar"),
      MoveUp,
      MoveRight,
      MoveRight,
      DownArray,
      DownField("foo")
    )

    val expected = "DecodingFailure at .foo.bar[0][2]: the message"
    assertEquals(DecodingFailure("the message", ops).show, expected)
  }

  property("Show[DecodingFailure] should display field selection") {
    Prop.forAll(downFields) { moves =>
      val selection = moves.foldRight("") {
        case (DownField(f), s) => s"$s.$f"
        case (_, s)            => throw new AssertionError("Impossible case")
      }

      val expected = s"DecodingFailure at $selection: the message"
      DecodingFailure("the message", moves).show ?= expected
    }
  }

  property("Show[DecodingFailure] should display array indexing") {
    Prop.forAll(arrayMoves) { moves =>
      val ops = moves :+ DownArray
      val index = moves.foldLeft(0) {
        case (i, MoveLeft)  => i - 1
        case (i, MoveRight) => i + 1
        case (i, _)         => i
      }

      val expected = s"DecodingFailure at [$index]: the message"
      DecodingFailure("the message", ops).show ?= expected
    }
  }

  test("Failing error messages on decoders should be of the typical format.") {
    val json: Json =
      Json.fromJsonObject(
        JsonObject(
          "derp" -> Json.fromInt(1)
        )
      )

    assertEquals(
      Decoder[TestData].decodeJson(json).leftMap(_.show),
      Left("DecodingFailure at .foo: Missing required field")
    )
  }
}

object ShowErrorSuite {
  final case class TestData(foo: String, bar: Int)

  object TestData {
    implicit val decoder: Decoder[TestData] =
      Decoder.forProduct2("foo", "bar")(TestData.apply _)
  }
}
