package io.circe.pointer.literal

import io.circe.pointer._
import munit.ScalaCheckSuite
import org.scalacheck.Prop

final class PointerInterpolatorCompileErrorsSuite extends ScalaCheckSuite {

  test("The pointer string interpolater should fail to compile on invalid literals") {
    assertNoDiff(
      compileErrors("pointer\"foo\""),
      s"""|error: Invalid JSON Pointer in interpolated string
          |      compileErrors("pointer\\"foo\\""),
          |                  ^
          |""".stripMargin
    )
  }

  test("The pointer string interpolater should fail with an interpolated relative distance") {
    assertNoDiff(
      compileErrors("""{val x = 1; pointer"$x/"}"""),
      s"""|error: Invalid JSON Pointer in interpolated string
          |      compileErrors(""\"{val x = 1; pointer"$$x/"}""\"),
          |                  ^
          |""".stripMargin
    )
  }

  test("The pointer string interpolater should fail with an empty interpolated relative distance") {
    assertNoDiff(
      compileErrors("""{val x = ""; pointer"$x/"}"""),
      s"""|error: Invalid JSON Pointer in interpolated string
          |      compileErrors(""\"{val x = ""; pointer"$$x/"}""\"),
          |                  ^
          |""".stripMargin
    )
  }
}
