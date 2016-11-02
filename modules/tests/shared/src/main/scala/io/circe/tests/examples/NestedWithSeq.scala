package io.circe.tests.examples

import cats.Eq
import io.circe.{ Decoder, Encoder }
import org.scalacheck.Arbitrary

object NestedWithSeqOuter {
  case class Inner(i: Int)
  object Inner {
    implicit val decodeInner: Decoder[Inner] = Decoder[Int].map(Inner(_))
    implicit val encodeInner: Encoder[Inner] = Encoder[Int].contramap(_.i)
    implicit val eqInner: Eq[Inner] = Eq.fromUniversalEquals
    implicit val arbitraryInner: Arbitrary[Inner] = Arbitrary(
      Arbitrary.arbitrary[Int].map(Inner(_))
    )
  }
}

case class NestedWithSeq(values: Seq[NestedWithSeqOuter.Inner])

object NestedWithSeq {
  val decodeNestedWithSeq: Decoder[NestedWithSeq] = Decoder.forProduct1("values")(NestedWithSeq.apply)
  val encodeNestedWithSeq: Encoder[NestedWithSeq] = Encoder.forProduct1("values")(_.values)
  implicit val eqNestedWithSeq: Eq[NestedWithSeq] = Eq.fromUniversalEquals
  implicit val arbitraryNestedWithSeq: Arbitrary[NestedWithSeq] = Arbitrary(
    Arbitrary.arbitrary[Seq[NestedWithSeqOuter.Inner]].map(NestedWithSeq(_))
  )
}
