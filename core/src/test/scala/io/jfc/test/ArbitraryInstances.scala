package io.jfc.test

import io.jfc.Json
import org.scalacheck.{ Arbitrary, Gen }

trait ArbitraryInstances {
  private[this] def maxDepth: Int = 5
  private[this] def maxSize: Int = 20

  private[this] def genNull: Gen[Json] = Gen.const(Json.empty)
  private[this] def genBool: Gen[Json] = Arbitrary.arbBool.arbitrary.map(Json.bool)

  private[this] def genNumber: Gen[Json] = Gen.oneOf(
    Arbitrary.arbLong.arbitrary.map(Json.long),
    Arbitrary.arbDouble.arbitrary.map(Json.numberOrNull)
  )
  private[this] def genString: Gen[Json] = Arbitrary.arbString.arbitrary.map(Json.string)

  private[this] def genArray(depth: Int): Gen[Json] = Gen.choose(0, maxSize).flatMap { size =>
    Gen.listOfN(
      size,
      arbitraryJsonAtDepth(depth + 1).arbitrary
    ).map(Json.array)
  }

  private[this] def genObject(depth: Int): Gen[Json] = Gen.choose(0, maxSize).flatMap { size =>
    Gen.listOfN(
      size,
      for {
        k <- Arbitrary.arbString.arbitrary
        v <- arbitraryJsonAtDepth(depth + 1).arbitrary
      } yield k -> v
    ).map(Json.obj)
  }

  private[this] def arbitraryJsonAtDepth(depth: Int): Arbitrary[Json] = {
    val genJsons = List( genNumber, genString) ++ (
      if (depth < maxDepth) List(genArray(depth), genObject(depth)) else Nil
    )

    Arbitrary(Gen.oneOf(genNull, genBool, genJsons: _*))
  }

  implicit def arbitraryJson: Arbitrary[Json] = arbitraryJsonAtDepth(0)
}
