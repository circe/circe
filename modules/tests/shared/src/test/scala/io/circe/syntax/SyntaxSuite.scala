package io.circe.syntax

import cats.kernel.instances.string._
import cats.kernel.instances.tuple._
import cats.syntax.eq._
import io.circe.{ Encoder, Json, KeyEncoder }
import io.circe.tests.CirceMunitSuite
import org.scalacheck.Prop.forAll

class SyntaxSuite extends CirceMunitSuite {
  property("asJson should be available and work appropriately") {
    forAll { (s: String) =>
      assert(s.asJson === Json.fromString(s))
    }
  }

  property("asJsonObject should be available and work appropriately") {
    forAll { (m: Map[String, Int]) =>
      assert(m.asJsonObject === Encoder[Map[String, Int]].apply(m).asObject.get)
    }
  }

  property(":= should be available and work with String keys") {
    forAll { (key: String, m: Map[String, Int], aNumber: Int, aString: String, aBoolean: Boolean) =>
      assert((key := m) === (key -> m.asJson))
      assert((key := aNumber) === (key -> aNumber.asJson))
      assert((key := aString) === (key -> aString.asJson))
      assert((key := aBoolean) === (key -> aBoolean.asJson))
    }
  }

  property(":= should be available and work with non-String keys that have a KeyEncoder instance") {
    case class CustomKey(componentOne: String, componentTwo: Int)
    implicit val keyEncoder: KeyEncoder[CustomKey] =
      KeyEncoder[String].contramap(k => s"${k.componentOne}_${k.componentTwo}")

    forAll {
      (
        m: Map[String, Int],
        aNumber: Int,
        aString: String,
        aBoolean: Boolean
      ) =>
        val key = CustomKey("keyComponentOne", 2)
        val keyStringRepresentation = "keyComponentOne_2"
        assert((key := m) === (keyStringRepresentation -> m.asJson))
        assert((key := aNumber) === (keyStringRepresentation -> aNumber.asJson))
        assert((key := aString) === (keyStringRepresentation -> aString.asJson))
        assert((key := aBoolean) === (keyStringRepresentation -> aBoolean.asJson))
    }
  }
}
