package io.circe

import org.scalacheck.Gen
import io.circe.CursorOp._
import io.circe.tests.CirceSuite

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

class ShowErrorSuite extends CirceSuite with GenCursorOps {
  "Show[ParsingFailure]" should "produce the expected output" in {
    assert(ParsingFailure("the message", new Exception()).show === "ParsingFailure: the message")
  }

  "Show[DecodingFailure]" should "produce the expected output on a small example" in {
    val ops = List(MoveRight, MoveRight, DownArray, DownField("bar"), DownField("foo"))

    assert(DecodingFailure("the message", ops).show === "DecodingFailure at .foo.bar[2]: the message")
  }

  it should "produce the expected output on a larger example" in {
    val ops = List(
      DeleteGoFirst,
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

    val expected = "DecodingFailure at .foo.bar[0][2]{|<-!}: the message"
    assert(DecodingFailure("the message", ops).show === expected)
  }

  it should "display field selection" in forAll(downFields) { moves =>
    val selection = moves.foldRight("") {
      case (DownField(f), s) => s"$s.$f"
      case (_, s)            => s
    }

    assert(DecodingFailure("the message", moves).show === s"DecodingFailure at $selection: the message")
  }

  it should "display array indexing" in forAll(arrayMoves) { moves =>
    val ops = moves :+ DownArray
    val index = moves.foldLeft(0) {
      case (i, MoveLeft)  => i - 1
      case (i, MoveRight) => i + 1
      case (i, LeftN(n))  => i - n
      case (i, RightN(n)) => i + n
      case (i, _)         => i
    }

    assert(DecodingFailure("the message", ops).show === s"DecodingFailure at [$index]: the message")
  }
}
