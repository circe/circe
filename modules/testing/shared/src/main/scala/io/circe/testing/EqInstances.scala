package io.circe.testing
import cats.kernel.Eq
import io.circe.{ Decoder, Encoder, Json, KeyDecoder, KeyEncoder }
import org.scalacheck.Arbitrary

trait EqInstances { this: ArbitraryInstances =>

  /**
   * The number of arbitrary values that will be considered when checking for
   * codec equality.
   */
  protected def codecEqualityCheckCount: Int = 16

  private[this] def arbitraryValues[A](implicit A: Arbitrary[A]): Stream[A] = Stream
    .continually(
      A.arbitrary.sample
    )
    .flatten

  implicit def eqKeyEncoder[A: Arbitrary]: Eq[KeyEncoder[A]] = Eq.instance { (e1, e2) =>
    arbitraryValues[A].take(codecEqualityCheckCount).forall(a => Eq[String].eqv(e1(a), e2(a)))
  }

  implicit def eqKeyDecoder[A: Eq]: Eq[KeyDecoder[A]] = Eq.instance { (d1, d2) =>
    arbitraryValues[String].take(codecEqualityCheckCount).forall(s => Eq[Option[A]].eqv(d1(s), d2(s)))
  }

  implicit def eqEncoder[A: Arbitrary]: Eq[Encoder[A]] = Eq.instance { (e1, e2) =>
    arbitraryValues[A].take(codecEqualityCheckCount).forall(a => Eq[Json].eqv(e1(a), e2(a)))
  }

  implicit def eqDecoder[A: Eq]: Eq[Decoder[A]] = Eq.instance { (d1, d2) =>
    arbitraryValues[Json]
      .take(codecEqualityCheckCount)
      .forall(json => Eq[Decoder.Result[A]].eqv(d1(json.hcursor), d2(json.hcursor)))
  }

  implicit def eqAsObjectEncoder[A: Arbitrary]: Eq[Encoder.AsObject[A]] = Eq.instance { (e1, e2) =>
    arbitraryValues[A].take(codecEqualityCheckCount).forall(a => Eq[Json].eqv(e1(a), e2(a)))
  }

  implicit def eqAsArrayEncoder[A: Arbitrary]: Eq[Encoder.AsArray[A]] = Eq.instance { (e1, e2) =>
    arbitraryValues[A].take(codecEqualityCheckCount).forall(a => Eq[Json].eqv(e1(a), e2(a)))
  }
}
