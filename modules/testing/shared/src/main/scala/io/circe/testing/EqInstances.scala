package io.circe.testing

import cats.Eq
import cats.instances.either._
import cats.syntax.eq._
import io.circe.{ AccumulatingDecoder, Decoder, Encoder, Json, ObjectEncoder }
import org.scalacheck.Arbitrary

trait EqInstances { this: ArbitraryInstances =>
  /**
   * The number of arbitrary values that will be considered when checking for
   * codec equality.
   */
  protected def codecEqualityCheckCount: Int = 16

  private[this] def arbitraryValues[A](implicit A: Arbitrary[A]): Stream[A] = Stream.continually(
    A.arbitrary.sample
  ).flatten

  implicit def eqEncoder[A: Arbitrary]: Eq[Encoder[A]] = Eq.instance { (e1, e2) =>
    arbitraryValues[A].take(codecEqualityCheckCount).forall(a => e1(a) === e2(a))
  }

  implicit def eqDecoder[A: Eq]: Eq[Decoder[A]] = Eq.instance { (d1, d2) =>
    arbitraryValues[Json].take(codecEqualityCheckCount).forall(json => d1(json.hcursor) === d2(json.hcursor))
  }

  implicit def eqObjectEncoder[A: Arbitrary]: Eq[ObjectEncoder[A]] = Eq.instance { (e1, e2) =>
    arbitraryValues[A].take(codecEqualityCheckCount).forall(a => e1(a) === e2(a))
  }

  implicit def eqAccumulatingDecoder[A: Eq]: Eq[AccumulatingDecoder[A]] = Eq.instance { (d1, d2) =>
    arbitraryValues[Json].take(codecEqualityCheckCount).forall(json => d1(json.hcursor) === d2(json.hcursor))
  }
}
