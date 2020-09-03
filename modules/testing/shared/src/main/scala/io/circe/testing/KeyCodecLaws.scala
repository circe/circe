package io.circe.testing
import cats.kernel.Eq
import cats.kernel.laws.{ IsEq, SerializableLaws }
import cats.laws._
import cats.laws.discipline._
import io.circe.{ KeyDecoder, KeyEncoder }
import org.scalacheck.{ Arbitrary, Prop, Shrink }
import org.typelevel.discipline.Laws

trait KeyCodecLaws[A] {

  def decodeKey: KeyDecoder[A]
  def encodeKey: KeyEncoder[A]

  def keyCodecRoundTrip(a: A): IsEq[Option[A]] =
    decodeKey(encodeKey(a)) <-> Some(a)

}

object KeyCodecLaws {
  def apply[A: KeyDecoder: KeyEncoder]: KeyCodecLaws[A] = new KeyCodecLaws[A] {
    override val decodeKey: KeyDecoder[A] = KeyDecoder[A]
    override val encodeKey: KeyEncoder[A] = KeyEncoder[A]
  }
}

trait KeyCodecTests[A] extends Laws {
  def laws: KeyCodecLaws[A]

  def keyCodec(implicit
    arbitraryA: Arbitrary[A],
    shrinkA: Shrink[A],
    eqA: Eq[A],
    arbitraryString: Arbitrary[String],
    shrinkString: Shrink[String]
  ): RuleSet = new DefaultRuleSet(
    name = "keyCodec",
    parent = None,
    "roundTrip" -> Prop.forAll { (a: A) =>
      laws.keyCodecRoundTrip(a)
    },
    "keyDecoder serializability" -> SerializableLaws.serializable(laws.decodeKey),
    "keyEncoder serializability" -> SerializableLaws.serializable(laws.encodeKey)
  )

  def unserializableKeyCodec(implicit
    arbitraryA: Arbitrary[A],
    shrinkA: Shrink[A],
    eqA: Eq[A],
    arbitraryString: Arbitrary[String],
    shrinkString: Shrink[String]
  ): RuleSet = new DefaultRuleSet(
    name = "keyCodec",
    parent = None,
    "roundTrip" -> Prop.forAll { (a: A) =>
      laws.keyCodecRoundTrip(a)
    }
  )
}

object KeyCodecTests {
  def apply[A: KeyDecoder: KeyEncoder]: KeyCodecTests[A] = new KeyCodecTests[A] {
    override def laws: KeyCodecLaws[A] = KeyCodecLaws[A]
  }
}
