package io.circe

import cats.kernel.instances.string._
import cats.syntax.eq._
import cats.syntax.show._
import io.circe.CursorOp._
import io.circe.tests.CirceSuite
import munit.ScalaCheckSuite
import org.scalacheck.{ Gen, Prop }

trait GenCursorOps {
  val arrayMoves: Gen[List[CursorOp]] = Gen.listOf(
    Gen.oneOf(
      Gen.const(MoveLeft),
      Gen.const(MoveRight),
      Gen.choose(1, 10000).map(LeftN),
      Gen.choose(1, 10000).map(RightN)
    )
  )

  val downFields: Gen[List[CursorOp]] = Gen.listOf(Gen.identifier.map(DownField))
}

class ShowErrorSuite extends ScalaCheckSuite with GenCursorOps {
  test("Show[ParsingFailure] should produce the expected output") {
    assert(ParsingFailure("the message", new Exception()).show === "ParsingFailure: the message")
  }

  test("Show[DecodingFailure] should produce the expected output on a small example") {
    val ops = List(MoveRight, MoveRight, DownArray, DownField("bar"), DownField("foo"))

    assert(DecodingFailure("the message", ops).show === "DecodingFailure at .foo.bar[2]: the message")
  }

  test("Show[DecodingFailure] should produce the expected output on a larger example") {
    val ops = List(
      MoveLeft,
      LeftN(2),
      RightN(5),
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
    assert(DecodingFailure("the message", ops).show === expected)
  }

  property("Show[DecodingFailure] should display field selection") {
    Prop.forAll(downFields) { moves =>
      val selection = moves.foldRight("") {
        case (DownField(f), s) => s"$s.$f"
        case (_, s)            => s
      }

      val expected = s"DecodingFailure at $selection: the message"
      DecodingFailure("the message", moves).show === expected
    }
  }

  property("Show[DecodingFailure] should display array indexing") {
    Prop.forAll(arrayMoves) { moves =>
      val ops = moves :+ DownArray
      val index = moves.foldLeft(0) {
        case (i, MoveLeft)  => i - 1
        case (i, MoveRight) => i + 1
        case (i, LeftN(n))  => i - n
        case (i, RightN(n)) => i + n
        case (i, _)         => i
      }

      val expected = s"DecodingFailure at [$index]: the message"
      DecodingFailure("the message", ops).show === expected
    }
  }
}
