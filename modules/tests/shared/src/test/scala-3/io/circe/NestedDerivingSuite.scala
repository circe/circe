package io.circe

import io.circe.tests.CirceMunitSuite
import cats.Functor
import cats.implicits.*
import cats.data.{ Chain, NonEmptyChain, NonEmptyList, NonEmptyMap, NonEmptySet, NonEmptyVector }
import cats.kernel.Order
import io.circe.syntax.*
import org.scalacheck.Prop.forAll
import org.scalacheck.{ Gen, Prop }
import org.typelevel.discipline.Laws

import scala.collection.immutable.SortedSet

class NestedDerivingSuite extends CirceMunitSuite {
  case class Inner(a: Long)
  val genInner: Gen[Inner] = Gen.long.map(Inner.apply)
  implicit val innerOrder: Order[Inner] = Order.by(_.a)

  // Option
  case class OuterOption(inner: Option[Inner])
  implicit val genOuterOption: Gen[OuterOption] = Gen.option(genInner).map(OuterOption.apply)
  property("Option[A] Encoder derivation should not depend on not whether A is derived or not") {
    val derivedEncoder: Encoder[OuterOption] = {
      implicit val enc: Encoder[Inner] = Encoder.AsObject.derived
      Encoder.AsObject.derived
    }
    val nonDerivedEncoder: Encoder[OuterOption] = Encoder.AsObject.derived

    forAll(genOuterOption) { outer => assert(derivedEncoder(outer) == nonDerivedEncoder(outer)) }
  }
  property("Option[A] Decoder derivation should not depend on not whether A is derived or not") {
    val derivedDecoder: Decoder[OuterOption] = {
      implicit val enc: Decoder[Inner] = Decoder.derived
      Decoder.derived
    }
    val nonDerivedDecoder: Decoder[OuterOption] = Decoder.derived

    forAll(genOuterOption) { outer =>
      val json = Json.obj("inner" -> outer.inner.map(i => Json.obj("a" -> i.a.asJson)).asJson)
      assert(derivedDecoder(json.hcursor) == nonDerivedDecoder(json.hcursor))
    }
  }

  // List
  case class OuterList(inner: List[Inner])
  implicit val genOuterList: Gen[OuterList] = Gen.listOf(genInner).map(OuterList.apply)
  property("List[A] Encoder derivation should not depend on not whether A is derived or not") {
    val derivedEncoder: Encoder[OuterList] = {
      implicit val enc: Encoder[Inner] = Encoder.AsObject.derived
      Encoder.AsObject.derived
    }
    val nonDerivedEncoder: Encoder[OuterList] = Encoder.AsObject.derived

    forAll(genOuterList) { outer => assert(derivedEncoder(outer) == nonDerivedEncoder(outer)) }
  }
  property("List[A] Decoder derivation should not depend on not whether A is derived or not") {
    val derivedDecoder: Decoder[OuterList] = {
      implicit val enc: Decoder[Inner] = Decoder.derived
      Decoder.derived
    }
    val nonDerivedDecoder: Decoder[OuterList] = Decoder.derived

    forAll(genOuterList) { outer =>
      val json = Json.obj("inner" -> outer.inner.map(i => Json.obj("a" -> i.a.asJson)).asJson)
      assert(derivedDecoder(json.hcursor) == nonDerivedDecoder(json.hcursor))
    }
  }

  // Vector
  case class OuterVector(inner: Vector[Inner])
  implicit val genOuterVector: Gen[OuterVector] = Gen.listOf(genInner).map(l => OuterVector(l.toVector))
  property("Vector[A] Encoder derivation should not depend on not whether A is derived or not") {
    val derivedEncoder: Encoder[OuterVector] = {
      implicit val enc: Encoder[Inner] = Encoder.AsObject.derived
      Encoder.AsObject.derived
    }
    val nonDerivedEncoder: Encoder[OuterVector] = Encoder.AsObject.derived

    forAll(genOuterVector) { outer => assert(derivedEncoder(outer) == nonDerivedEncoder(outer)) }
  }
  property("Vector[A] Decoder derivation should not depend on not whether A is derived or not") {
    val derivedDecoder: Decoder[OuterVector] = {
      implicit val enc: Decoder[Inner] = Decoder.derived
      Decoder.derived
    }
    val nonDerivedDecoder: Decoder[OuterVector] = Decoder.derived

    forAll(genOuterVector) { outer =>
      val json = Json.obj("inner" -> outer.inner.map(i => Json.obj("a" -> i.a.asJson)).asJson)
      assert(derivedDecoder(json.hcursor) == nonDerivedDecoder(json.hcursor))
    }
  }

  // Seq
  case class OuterSeq(inner: Seq[Inner])
  implicit val genOuterSeq: Gen[OuterSeq] = Gen.listOf(genInner).map(OuterSeq.apply)
  property("Seq[A] Encoder derivation should not depend on not whether A is derived or not") {
    val derivedEncoder: Encoder[OuterSeq] = {
      implicit val enc: Encoder[Inner] = Encoder.AsObject.derived
      Encoder.AsObject.derived
    }
    val nonDerivedEncoder: Encoder[OuterSeq] = Encoder.AsObject.derived

    forAll(genOuterSeq) { outer => assert(derivedEncoder(outer) == nonDerivedEncoder(outer)) }
  }
  property("Seq[A] Decoder derivation should not depend on not whether A is derived or not") {
    val derivedDecoder: Decoder[OuterSeq] = {
      implicit val enc: Decoder[Inner] = Decoder.derived
      Decoder.derived
    }
    val nonDerivedDecoder: Decoder[OuterSeq] = Decoder.derived

    forAll(genOuterSeq) { outer =>
      val json = Json.obj("inner" -> outer.inner.map(i => Json.obj("a" -> i.a.asJson)).asJson)
      assert(derivedDecoder(json.hcursor) == nonDerivedDecoder(json.hcursor))
    }
  }

  // Set
  case class OuterSet(inner: Set[Inner])
  implicit val genOuterSet: Gen[OuterSet] = Gen.listOf(genInner).map(list => OuterSet(list.toSet))
  property("Set[A] Encoder derivation should not depend on not whether A is derived or not") {
    val derivedEncoder: Encoder[OuterSet] = {
      implicit val enc: Encoder[Inner] = Encoder.AsObject.derived
      Encoder.AsObject.derived
    }
    val nonDerivedEncoder: Encoder[OuterSet] = Encoder.AsObject.derived

    forAll(genOuterSet) { outer => assert(derivedEncoder(outer) == nonDerivedEncoder(outer)) }
  }
  property("Set[A] Decoder derivation should not depend on not whether A is derived or not") {
    val derivedDecoder: Decoder[OuterSet] = {
      implicit val enc: Decoder[Inner] = Decoder.derived
      Decoder.derived
    }
    val nonDerivedDecoder: Decoder[OuterSet] = Decoder.derived

    forAll(genOuterSet) { outer =>
      val json = Json.obj("inner" -> outer.inner.map(i => Json.obj("a" -> i.a.asJson)).asJson)
      assert(derivedDecoder(json.hcursor) == nonDerivedDecoder(json.hcursor))
    }
  }

  // SortedSet
  case class OuterSortedSet(inner: SortedSet[Inner])
  implicit val genOuterSortedSet: Gen[OuterSortedSet] =
    Gen.listOf(genInner).map(list => OuterSortedSet(list.to(SortedSet)))
  property("SortedSet[A] Encoder derivation should not depend on not whether A is derived or not") {
    val derivedEncoder: Encoder[OuterSortedSet] = {
      implicit val enc: Encoder[Inner] = Encoder.AsObject.derived
      Encoder.AsObject.derived
    }
    val nonDerivedEncoder: Encoder[OuterSortedSet] = Encoder.AsObject.derived

    forAll(genOuterSortedSet) { outer => assert(derivedEncoder(outer) == nonDerivedEncoder(outer)) }
  }
  property("SortedSet[A] Decoder derivation should not depend on not whether A is derived or not") {
    val derivedDecoder: Decoder[OuterSortedSet] = {
      implicit val enc: Decoder[Inner] = Decoder.derived
      Decoder.derived
    }
    val nonDerivedDecoder: Decoder[OuterSortedSet] = Decoder.derived

    forAll(genOuterSet) { outer =>
      val json = Json.obj("inner" -> outer.inner.map(i => Json.obj("a" -> i.a.asJson)).asJson)
      assert(derivedDecoder(json.hcursor) == nonDerivedDecoder(json.hcursor))
    }
  }

  // Chain
  case class OuterChain(inner: Chain[Inner])
  implicit val genOuterChain: Gen[OuterChain] = Gen.listOf(genInner).map(list => OuterChain(Chain.fromSeq(list)))
  property("Chain[A] Encoder derivation should not depend on not whether A is derived or not") {
    val derivedEncoder: Encoder[OuterChain] = {
      implicit val enc: Encoder[Inner] = Encoder.AsObject.derived
      Encoder.AsObject.derived
    }
    val nonDerivedEncoder: Encoder[OuterChain] = Encoder.AsObject.derived

    forAll(genOuterChain) { outer => assert(derivedEncoder(outer) == nonDerivedEncoder(outer)) }
  }
  property("Chain[A] Decoder derivation should not depend on not whether A is derived or not") {
    val derivedDecoder: Decoder[OuterChain] = {
      implicit val enc: Decoder[Inner] = Decoder.derived
      Decoder.derived
    }
    val nonDerivedDecoder: Decoder[OuterChain] = Decoder.derived

    forAll(genOuterChain) { outer =>
      val json = Json.obj("inner" -> outer.inner.map(i => Json.obj("a" -> i.a.asJson)).asJson)
      assert(derivedDecoder(json.hcursor) == nonDerivedDecoder(json.hcursor))
    }
  }

  // NonEmptyVector
  case class OuterNonEmptyVector(inner: NonEmptyVector[Inner])
  implicit val genOuterNonEmptyVector: Gen[OuterNonEmptyVector] =
    for {
      head <- genInner
      tail <- Gen.listOf(genInner)
    } yield OuterNonEmptyVector(NonEmptyVector.of(head, tail*))
  property("NonEmptyVector[A] Encoder derivation should not depend on not whether A is derived or not") {
    val derivedEncoder: Encoder[OuterNonEmptyVector] = {
      implicit val enc: Encoder[Inner] = Encoder.AsObject.derived
      Encoder.AsObject.derived
    }
    val nonDerivedEncoder: Encoder[OuterNonEmptyVector] = Encoder.AsObject.derived

    forAll(genOuterNonEmptyVector) { outer => assert(derivedEncoder(outer) == nonDerivedEncoder(outer)) }
  }
  property("NonEmptyVector[A] Decoder derivation should not depend on not whether A is derived or not") {
    val derivedDecoder: Decoder[OuterNonEmptyVector] = {
      implicit val enc: Decoder[Inner] = Decoder.derived
      Decoder.derived
    }
    val nonDerivedDecoder: Decoder[OuterNonEmptyVector] = Decoder.derived

    forAll(genOuterNonEmptyVector) { outer =>
      val json = Json.obj("inner" -> outer.inner.map(i => Json.obj("a" -> i.a.asJson)).asJson)
      assert(derivedDecoder(json.hcursor) == nonDerivedDecoder(json.hcursor))
    }
  }
  /*
  // NonEmptySet
  case class OuterNonEmptySet(inner: NonEmptySet[Inner])
  implicit val genOuterNonEmptySet: Gen[OuterNonEmptySet] =
    for {
      head <- genInner
      tail <- Gen.listOf(genInner)
    } yield OuterNonEmptySet(NonEmptySet.of(head, tail*))
  property("NonEmptySet[A] Encoder derivation should not depend on not whether A is derived or not") {
    val derivedEncoder: Encoder[OuterNonEmptySet] = {
      implicit val enc: Encoder[Inner] = Encoder.AsObject.derived
      Encoder.AsObject.derived
    }
    val nonDerivedEncoder: Encoder[OuterNonEmptySet] = Encoder.AsObject.derived

    forAll(genOuterNonEmptySet) { outer => assert(derivedEncoder(outer) == nonDerivedEncoder(outer)) }
  }
  property("NonEmptySet[A] Decoder derivation should not depend on not whether A is derived or not") {
    val derivedDecoder: Decoder[OuterNonEmptySet] = {
      implicit val enc: Decoder[Inner] = Decoder.derived
      Decoder.derived
    }
    val nonDerivedDecoder: Decoder[OuterNonEmptySet] = Decoder.derived

    forAll(genOuterNonEmptySet) { outer =>
      val json = Json.obj("inner" -> outer.inner.toSortedSet.toSet.map(i => Json.obj("a" -> i.a.asJson)).asJson)
      assert(derivedDecoder(json.hcursor) == nonDerivedDecoder(json.hcursor))
    }
  }
   */
  // NonEmptyList
  case class OuterNonEmptyList(inner: NonEmptyList[Inner])
  implicit val genOuterNonEmptyList: Gen[OuterNonEmptyList] =
    for {
      head <- genInner
      tail <- Gen.listOf(genInner)
    } yield OuterNonEmptyList(NonEmptyList.of(head, tail*))
  property("NonEmptyList[A] Encoder derivation should not depend on not whether A is derived or not") {
    val derivedEncoder: Encoder[OuterNonEmptyList] = {
      implicit val enc: Encoder[Inner] = Encoder.AsObject.derived
      Encoder.AsObject.derived
    }
    val nonDerivedEncoder: Encoder[OuterNonEmptyList] = Encoder.AsObject.derived

    forAll(genOuterNonEmptyList) { outer => assert(derivedEncoder(outer) == nonDerivedEncoder(outer)) }
  }
  property("NonEmptyList[A] Decoder derivation should not depend on not whether A is derived or not") {
    val derivedDecoder: Decoder[OuterNonEmptyList] = {
      implicit val enc: Decoder[Inner] = Decoder.derived
      Decoder.derived
    }
    val nonDerivedDecoder: Decoder[OuterNonEmptyList] = Decoder.derived

    forAll(genOuterNonEmptyList) { outer =>
      val json = Json.obj("inner" -> outer.inner.map(i => Json.obj("a" -> i.a.asJson)).asJson)
      assert(derivedDecoder(json.hcursor) == nonDerivedDecoder(json.hcursor))
    }
  }
  /*
  // NonEmptyList
  case class OuterNonEmptyChain(inner: NonEmptyChain[Inner])
  implicit val genOuterNonEmptyChain: Gen[OuterNonEmptyChain] =
    for {
      head <- genInner
      tail <- Gen.listOf(genInner)
    } yield OuterNonEmptyChain(NonEmptyChain.of(head, tail*))
  property("NonEmptyChain[A] Encoder derivation should not depend on not whether A is derived or not") {
    val derivedEncoder: Encoder[OuterNonEmptyChain] = {
      implicit val enc: Encoder[Inner] = Encoder.AsObject.derived
      Encoder.AsObject.derived
    }
    val nonDerivedEncoder: Encoder[OuterNonEmptyChain] = Encoder.AsObject.derived

    forAll(genOuterNonEmptyChain) { outer => assert(derivedEncoder(outer) == nonDerivedEncoder(outer)) }
  }
  property("NonEmptyChain[A] Decoder derivation should not depend on not whether A is derived or not") {
    val derivedDecoder: Decoder[OuterNonEmptyChain] = {
      implicit val enc: Decoder[Inner] = Decoder.derived
      Decoder.derived
    }
    val nonDerivedDecoder: Decoder[OuterNonEmptyChain] = Decoder.derived

    forAll(genOuterNonEmptyChain) { outer =>
      val json = Json.obj("inner" -> outer.inner.map(i => Json.obj("a" -> i.a.asJson)).asJson)
      assert(derivedDecoder(json.hcursor) == nonDerivedDecoder(json.hcursor))
    }
  }
   */
  // Map
  case class OuterMap(inner: Map[Long, Inner])
  implicit val genOuterMap: Gen[OuterMap] =
    Gen.listOf(genInner).map(list => OuterMap(list.map(i => (i.a -> i)).toMap))
  property("Map[Long, A] Encoder derivation should not depend on not whether A is derived or not") {
    val derivedEncoder: Encoder[OuterMap] = {
      implicit val enc: Encoder[Inner] = Encoder.AsObject.derived
      Encoder.AsObject.derived
    }
    val nonDerivedEncoder: Encoder[OuterMap] = Encoder.AsObject.derived

    forAll(genOuterMap) { outer => assert(derivedEncoder(outer) == nonDerivedEncoder(outer)) }
  }
  property("Map[A] Decoder derivation should not depend on not whether A is derived or not") {
    val derivedDecoder: Decoder[OuterMap] = {
      implicit val enc: Decoder[Inner] = Decoder.derived
      Decoder.derived
    }
    val nonDerivedDecoder: Decoder[OuterMap] = Decoder.derived

    forAll(genOuterMap) { outer =>
      val json = Json.obj("inner" -> outer.inner.map {
        case (k, v) => Json.obj(k.toString -> Json.obj("a" -> v.a.asJson))
      }.asJson)
      assert(derivedDecoder(json.hcursor) == nonDerivedDecoder(json.hcursor))
    }
  }
  /*
  // NonEmptyMap
  case class OuterNonEmptyMap(inner: NonEmptyMap[Long, Inner])
  implicit val genOuterNonEmptyMap: Gen[OuterNonEmptyMap] =
    for {
      head <- genInner
      tail <- Gen.listOf(genInner)
      headElement = head.a -> head
      tailElements = tail.map(i => i.a -> i)
    } yield OuterNonEmptyMap(NonEmptyMap.of(headElement, tailElements*))
  property("NonEmptyMap[Long, A] Encoder derivation should not depend on not whether A is derived or not") {
    val derivedEncoder: Encoder[OuterNonEmptyMap] = {
      implicit val enc: Encoder[Inner] = Encoder.AsObject.derived
      Encoder.AsObject.derived
    }
    val nonDerivedEncoder: Encoder[OuterNonEmptyMap] = Encoder.AsObject.derived

    forAll(genOuterNonEmptyMap) { outer => assert(derivedEncoder(outer) == nonDerivedEncoder(outer)) }
  }
  property("NonEmptyMap[A] Decoder derivation should not depend on not whether A is derived or not") {
    val derivedDecoder: Decoder[OuterNonEmptyMap] = {
      implicit val enc: Decoder[Inner] = Decoder.derived
      Decoder.derived
    }
    val nonDerivedDecoder: Decoder[OuterNonEmptyMap] = Decoder.derived

    forAll(genOuterNonEmptyMap) { outer =>
      val json = Json.obj("inner" -> outer.inner.toSortedMap.toMap.map {
        case (k, v) => Json.obj(k.toString -> Json.obj("a" -> v.a.asJson))
      }.asJson)
      assert(derivedDecoder(json.hcursor) == nonDerivedDecoder(json.hcursor))
    }
  }
   */
}
