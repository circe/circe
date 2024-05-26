/*
 * Copyright 2024 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe

import cats.kernel.Eq
import cats.syntax.eq.*
import io.circe.{ Decoder, Encoder }
import io.circe.testing.CodecTests
import io.circe.tests.CirceMunitSuite
import org.scalacheck.{ Arbitrary, Gen }

object SemiautoDerivationSuite {
  case class Box[A](a: A)

  object Box {
    given decoder[A: Decoder]: Decoder[Box[A]] = Decoder.derived
    given encoder[A: Encoder]: Encoder.AsObject[Box[A]] = Encoder.AsObject.derived

    given eq[A: Eq]: Eq[Box[A]] = Eq.by(_.a)
    given arbitrary[A](using A: Arbitrary[A]): Arbitrary[Box[A]] = Arbitrary(A.arbitrary.map(Box(_)))
  }

  case class Foo(int: Int)

  object Foo {
    given decoder: Decoder[Foo] = Decoder.derived
    given encoder: Encoder.AsObject[Foo] = Encoder.AsObject.derived

    given eq: Eq[Foo] = Eq.by(_.int)
    given arbitrary: Arbitrary[Foo] = Arbitrary(Arbitrary.arbitrary[Int].map(Foo(_)))
  }

  case class Bar(foo: Box[Foo])

  object Bar {
    // We can derive a `Decoder` and `Encoder` for `Bar` because instances of each exist for `Foo`
    given decoder: Decoder[Bar] = Decoder.derived
    given encoder: Encoder.AsObject[Bar] = Encoder.AsObject.derived

    given eq: Eq[Bar] = Eq.by(_.foo)
    given arbitrary: Arbitrary[Bar] = Arbitrary(Arbitrary.arbitrary[Box[Foo]].map(Bar(_)))
  }

  case class Baz(str: String)

  // We cannot derive a `Decoder` or `Encoder` for `Quux` because no instances exist for `Baz`
  // see test below for proof that deriving instances fails to compile
  case class Quux(baz: Box[Baz])

  sealed trait Adt1
  object Adt1 {
    case class Class1(int: Int) extends Adt1
    object Class1 {
      given decoder: Decoder[Class1] = Decoder.derived
      given encoder: Encoder.AsObject[Class1] = Encoder.AsObject.derived

      given eq: Eq[Class1] = Eq.by(_.int)
      given arbitrary: Arbitrary[Class1] = Arbitrary(Arbitrary.arbitrary[Int].map(Class1(_)))
    }

    case object Object1 extends Adt1

    // We can derive a `Decoder` and `Encoder` for `Adt1` because instances of each exist for `Class1`
    given decoder: Decoder[Adt1] = Decoder.derived
    given encoder: Encoder.AsObject[Adt1] = Encoder.AsObject.derived

    given eq: Eq[Adt1] = Eq.instance {
      case (x: Class1, y: Class1) => x === y
      case (Object1, Object1)     => true
      case _                      => false
    }
    given arbitrary: Arbitrary[Adt1] = Arbitrary(Gen.oneOf(Arbitrary.arbitrary[Class1], Gen.const(Object1)))
  }

  sealed trait Adt2
  object Adt2 {
    case object Object1 extends Adt2
    case object Object2 extends Adt2

    // We can derive a `Decoder` and `Encoder` for `Adt2` because its members are all `case object`s
    given decoder: Decoder[Adt2] = Decoder.derived
    given encoder: Encoder.AsObject[Adt2] = Encoder.AsObject.derived

    given eq: Eq[Adt2] = Eq.fromUniversalEquals
    given arbitrary: Arbitrary[Adt2] = Arbitrary(Gen.oneOf(Gen.const(Object1), Gen.const(Object2)))
  }

  sealed trait Adt3
  object Adt3 {
    case class Class1() extends Adt3
    case object Object1 extends Adt3

    given decoder: Decoder[Adt3] = Decoder.derived
    given encoder: Encoder.AsObject[Adt3] = Encoder.AsObject.derived

    given eq: Eq[Adt3] = Eq.fromUniversalEquals
    given arbitrary: Arbitrary[Adt3] = Arbitrary(Gen.oneOf(Gen.const(Class1()), Gen.const(Object1)))
  }

  sealed trait Adt4
  object Adt4 {
    sealed trait SubTrait1 extends Adt4
    case class Class1() extends SubTrait1
    sealed trait SubTrait2 extends Adt4
    case object Object1 extends SubTrait2

    given decoder: Decoder[Adt4] = Decoder.derived
    given encoder: Encoder.AsObject[Adt4] = Encoder.AsObject.derived

    given eq: Eq[Adt4] = Eq.fromUniversalEquals
    given arbitrary: Arbitrary[Adt4] = Arbitrary(Gen.oneOf(Gen.const(Class1()), Gen.const(Object1)))
  }

  sealed trait Adt5
  object Adt5 {
    case class Nested()
    case class Class1(nested: Nested) extends Adt5
    case object Object1 extends Adt5

    // We cannot derive a `Decoder` or `Encoder` for `Adt5` because no instances exist for `Nested`
    // see test below for proof that deriving instances fails to compile
  }
}

class SemiautoDerivationSuite extends CirceMunitSuite {
  import SemiautoDerivationSuite.*

  checkAll("Codec[Foo]", CodecTests[Foo].codec)
  checkAll("Codec[Box[Foo]]", CodecTests[Box[Foo]].codec)
  checkAll("Codec[Bar]", CodecTests[Bar].codec)
  checkAll("Codec[Box[Bar]]", CodecTests[Box[Bar]].codec)
  checkAll("Codec[Adt1]", CodecTests[Adt1].codec)
  checkAll("Codec[Adt2]", CodecTests[Adt2].codec)
  checkAll("Codec[Adt3]", CodecTests[Adt3].codec)
  checkAll("Codec[Adt4]", CodecTests[Adt4].codec)

  test("Nested case classes cannot be derived") {
    assert(compileErrors("Decoder.derived[Quux]").nonEmpty)
    assert(compileErrors("Encoder.AsObject.derived[Quux]").nonEmpty)
  }

  test("Nested ADTs cannot be derived") {
    assert(compileErrors("Decoder.derived[Adt5]").nonEmpty)
    assert(compileErrors("Encoder.AsObject.derived[Adt5]").nonEmpty)
  }
}
