package io.circe.pointer.literal

import io.circe.pointer.Pointer
import munit.ScalaCheckSuite
import org.scalacheck.Prop
import io.circe.pointer.ScalaCheckInstances

final class PointerInterpolatorCompileErrorsSuite extends ScalaCheckSuite {

  test("The pointer string interpolater should fail to compile on invalid literals") {
    assertNoDiff(
      compileErrors("pointer\"foo\""),
      """|error: Invalid JSON Pointer in interpolated string
         |pointer"foo"
         |^
         |""".stripMargin
    )
  }

  test("The pointer string interpolater should fail with an interpolated relative distance") {
    assertNoDiff(
      compileErrors("""{val x = 1; pointer"$x/"}"""),
      """|error: Invalid JSON Pointer in interpolated string
         |{val x = 1; pointer"$x/"}
         |            ^
         |""".stripMargin
    )
  }

  test("The pointer string interpolater should fail with an empty interpolated relative distance") {
    assertNoDiff(
      compileErrors("""{val x = ""; pointer"$x/"}"""),
      """|error: Invalid JSON Pointer in interpolated string
         |{val x = ""; pointer"$x/"}
         |             ^
         |""".stripMargin
    )
  }
}
