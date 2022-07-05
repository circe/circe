package io.circe

import cats.data.Chain
import cats.kernel.instances.float._
import cats.kernel.instances.int._
import cats.kernel.instances.list._
import cats.kernel.instances.map._
import cats.kernel.instances.string._
import cats.kernel.instances.tuple._
import cats.laws.discipline.arbitrary._
import cats.laws.discipline.ContravariantTests
import cats.syntax.eq._
import io.circe.syntax._
import io.circe.tests.CirceMunitSuite
import org.scalacheck.Arbitrary
import org.scalacheck.Prop.forAll
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

    assert(Decoder[Map[String, Int]].apply(newEncoder(m).hcursor) === Right(m.updated(k, v)))
  }

  property("Encoder.AsObject#mapJsonObject should transform encoded output") {
    forAll { (m: Map[String, Int], k: String, v: Int) =>
      val newEncoder = Encoder.AsObject[Map[String, Int]].mapJsonObject(_.add(k, v.asJson))

      assert(Decoder[Map[String, Int]].apply(newEncoder(m).hcursor) === Right(m.updated(k, v)))
    }
  }

  property("encodeSet should match sequence encoders") {
    forAll { (xs: Set[Int]) =>
      assert(Encoder.encodeSet[Int].apply(xs) === Encoder[Seq[Int]].apply(xs.toSeq))
    }
  }

  property("encodeList should match sequence encoders") {
    forAll { (xs: List[Int]) =>
      assert(Encoder.encodeList[Int].apply(xs) === Encoder[Seq[Int]].apply(xs))
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

    assert(asJsonObject.toList === expected)
  }

  property("encodeVector should match sequence encoders") {
    forAll { (xs: Vector[Int]) =>
      assert(Encoder.encodeVector[Int].apply(xs) === Encoder[Seq[Int]].apply(xs))
    }
  }

  property("encodeChain should match sequence encoders") {
    forAll { (xs: Chain[Int]) =>
      assert(Encoder.encodeChain[Int].apply(xs) === Encoder[Seq[Int]].apply(xs.toList))
    }
  }

  property("encodeFloat should match string representation")(encodeFloatPreserveStringProp)
  lazy val encodeFloatPreserveStringProp = forAll { (x: Float) =>
    // All Float values should be encoded in a way that match the original value.
    assert(Encoder[Float].apply(x).toString.toFloat === x)

    // For floats which are NOT represented with scientific notation,
    // the JSON representation should match Float.toString
    // This should catch cases where 1.2f would previously be encoded
    // as 1.2000000476837158 due to the use of .toDouble
    if (!x.toString.toLowerCase.contains('e')) {
      assert(Encoder[Float].apply(x).toString === x.toString)
    }
  }
}
