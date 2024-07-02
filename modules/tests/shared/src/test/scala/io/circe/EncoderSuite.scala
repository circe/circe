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

import cats.data.Chain
import cats.kernel.instances.float._
import cats.laws.discipline.arbitrary._
import cats.laws.discipline.{ ContravariantTests, DeferTests, MiniInt }
import cats.syntax.eq._
import io.circe.syntax._
import io.circe.tests.CirceMunitSuite
import org.scalacheck.Arbitrary
import org.scalacheck._
import org.scalacheck.Prop._

import scala.collection.SortedMap

class EncoderSuite extends CirceMunitSuite {
  checkAll("Encoder[Int]", ContravariantTests[Encoder].contravariant[Int, Int, Int])
  checkAll("Encoder.AsArray[Int]", ContravariantTests[Encoder.AsArray].contravariant[Int, Int, Int])
  checkAll("Encoder.AsObject[Int]", ContravariantTests[Encoder.AsObject].contravariant[Int, Int, Int])

  property("mapJson should transform encoded output")(mapJsonTransformProp)
  private lazy val mapJsonTransformProp = forAll { (m: Map[String, Int], k: String, v: Int) =>
    val newEncoder = Encoder[Map[String, Int]].mapJson(
      _.withObject(obj => Json.fromJsonObject(obj.add(k, v.asJson)))
    )

    Decoder[Map[String, Int]].apply(newEncoder(m).hcursor) ?= Right(m.updated(k, v))
  }

  property("Encoder.AsObject#mapJsonObject should transform encoded output") {
    forAll { (m: Map[String, Int], k: String, v: Int) =>
      val newEncoder = Encoder.AsObject[Map[String, Int]].mapJsonObject(_.add(k, v.asJson))

      Decoder[Map[String, Int]].apply(newEncoder(m).hcursor) ?= Right(m.updated(k, v))
    }
  }

  property("encodeSet should match sequence encoders") {
    forAll { (xs: Set[Int]) =>
      Encoder.encodeSet[Int].apply(xs) ?= Encoder[Seq[Int]].apply(xs.toSeq)
    }
  }

  property("encodeList should match sequence encoders") {
    forAll { (xs: List[Int]) =>
      Encoder.encodeList[Int].apply(xs) ?= Encoder[Seq[Int]].apply(xs)
    }
  }

  case class MyString(value: String)

  object MyString {
    implicit val myStringOrdering: Ordering[MyString] = Ordering.by[MyString, String](_.value).reverse
    implicit val myStringKeyEncoder: KeyEncoder[MyString] = KeyEncoder.instance(_.value)
    implicit val myStringArbitrary: Arbitrary[MyString] = Arbitrary(
      Arbitrary.arbitrary[String].map(MyString(_))
    )
  }

  property("encodeMap should preserve insertion order")(encodeMapProp)
  private lazy val encodeMapProp = forAll { (m: SortedMap[MyString, String]) =>
    val Some(asJsonObject) = m.asJson.asObject
    val expected = m.toList.map {
      case (k, v) => MyString.myStringKeyEncoder(k) -> Json.fromString(v)
    }

    asJsonObject.toList ?= expected
  }

  property("encodeVector should match sequence encoders") {
    forAll { (xs: Vector[Int]) =>
      Encoder.encodeVector[Int].apply(xs) ?= Encoder[Seq[Int]].apply(xs)
    }
  }

  property("encodeChain should match sequence encoders") {
    forAll { (xs: Chain[Int]) =>
      Encoder.encodeChain[Int].apply(xs) ?= Encoder[Seq[Int]].apply(xs.toList)
    }
  }

  // https://docs.oracle.com/javase/8/docs/api/java/lang/Float.html#toString-float-
  val genScientificFloat: Gen[Float] =
    Gen
      .oneOf(
        Gen.choose(Float.MinValue, 1e-3f),
        Gen.choose(1e7f, Float.MaxValue)
      )
      .suchThat((f: Float) => f =!= 1e-3f)
      .suchThat((f: Float) => f =!= 1e7f)

  property("encodeFloat should match string representation, when in scientific notation")(forAll(genScientificFloat) {
    (x: Float) =>
      // For floats which are NOT represented with scientific notation,
      // the JSON representation should match Float.toString
      // This should catch cases where 1.2f would previously be encoded
      // as 1.2000000476837158 due to the use of .toDouble
      if (x.toString.toLowerCase.contains('e')) {
        Encoder[Float].apply(x).toString ?= x.toString
      } else {
        Prop.falsified :| "Generated float value which was not represented by Scientific notation, this should not be possible with this generator."
      }
  })

  property("encodeFloat should match string representation")(forAll { (x: Float) =>
    // All Float values should be encoded in a way that match the original value.
    Encoder[Float].apply(x).toString.toFloat ?= x
  })

  checkAll("Defer[Encoder]", DeferTests[Encoder].defer[MiniInt])

  test("Encoder.recursive should prevent undesirable grown in the number of instances created") {
    var counter = 0

    implicit def uglyListEncoder[A: Encoder]: Encoder[List[A]] = {
      counter += 1
      Encoder.recursive[List[A]] { implicit recurse =>
        Encoder.instance {
          case Nil        => Json.Null
          case car :: Nil => Json.obj("car" := car.asJson)
          case car :: cdr => Json.obj("car" := car.asJson, "cdr" := cdr.asJson)
        }
      }
    }

    assertEquals(
      (0 :: 1 :: 2 :: 3 :: Nil).asJson,
      Json.obj(
        "car" := 0,
        "cdr" := Json.obj(
          "car" := 1,
          "cdr" := Json.obj(
            "car" := 2,
            "cdr" := Json.obj(
              "car" := 3
            )
          )
        )
      )
    )
    // Without `Encoder.recursive`, this would create 4 instances of an `Encoder[List[Int]]`
    assertEquals(counter, 1)
  }
}
