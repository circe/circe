package io.circe.generic.util

import io.circe.tests.CirceSuite
import org.scalacheck.Prop.forAll
import shapeless.{ test => _, _ }, shapeless.record._, shapeless.syntax.singleton._

class ComplementSuite extends CirceSuite {
  type R = Record.`'i -> Int, 's -> String, 'c -> Char`.T

  test("Remove and reinsert labeled elements") {
    check {
      forAll { (i: Int, s: String, c: Char) =>
        type A = Record.`'i -> Int, 's -> String`.T
        type L = Record.`'c -> Char`.T

        val r = 'i ->> i :: 's ->> s :: 'c ->> c :: HNil

        val leftover: L = Complement[R, A].apply(r)
        val recovered: R = Complement[R, A].insert('i ->> i :: 's ->> s :: HNil, leftover)

        leftover('c) === c &&
          recovered('i) === r('i) && recovered('s) === r('s) && recovered('c) === r('c)
      }
    }
  }

  test("Remove and reinsert unlabeled elements") {
    check {
      forAll { (i: Int, s: String, c: Char) =>
        type A = Int :: String :: HNil
        type L = Record.`'c -> Char`.T

        val r = 'i ->> i :: 's ->> s :: 'c ->> c :: HNil

        val leftover: L = Complement[R, A].apply(r)
        val recovered: R = Complement[R, A].insert(i :: s :: HNil, leftover)

        leftover('c) === c &&
          recovered('i) === r('i) && recovered('s) === r('s) && recovered('c) === r('c)
      }
    }
  }
}
