package io.circe

import java.{ math => jm }
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.time.temporal.ChronoUnit.MILLIS

import scala.quoted.{ Quotes, staging }

import io.circe.tests.CirceMunitSuite
import org.scalacheck.{ Arbitrary, Gen }

object ExplicitNullsDerivesSuite {
  case class Foo(i: jm.BigInteger, j: Int, s: String)
  case class Bar[A](a: A)

  case class Event(id: Int, time: Instant)
}

class ExplicitNullsDerivesSuite extends CirceMunitSuite {
  import ExplicitNullsDerivesSuite._

  val settings = staging.Compiler.Settings.make(compilerArgs = List("-Yexplicit-nulls"))
  given explicitNullsCompiler: staging.Compiler = staging.Compiler.make(getClass.getClassLoader)(settings)

  test("Derives under `-Yexplicit-nulls` should be compilable") {
    def code(using Quotes) = '{
      given Encoder[Bar[Foo]] = Encoder.AsObject.derived[Bar[Foo]]
      given Decoder[Bar[Foo]] = Decoder.derived[Bar[Foo]]
      given Codec[Bar[Foo]] = Codec.AsObject.derived[Bar[Foo]]
    }

    staging.run(code)
  }

  test("Java boxed class would require `.nn` postfix with `-Yexplicit-nulls`") {
    def code(using Quotes) = '{
      given Encoder[Instant] = Encoder.encodeString.contramap[Instant](t =>
        ISO_OFFSET_DATE_TIME.nn
          .format(
            t.nn.truncatedTo(MILLIS).nn.atOffset(ZoneOffset.UTC)
          )
          .nn
      )
      given Codec[Event] = Codec.AsObject.derived[Event]
    }

    staging.run(code)
  }
}
