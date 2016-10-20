package io.circe

import cats.data.{ NonEmptyList, NonEmptyVector, OneAnd, Validated }
import cats.functor.Contravariant
import cats.Foldable
import io.circe.export.Exported
import java.util.UUID
import scala.collection.GenSeq
import scala.collection.generic.IsTraversableOnce

/**
 * A type class that provides a conversion from a value of type `A` to a [[Json]] value.
 *
 * @author Travis Brown
 */
trait Encoder[A] extends Serializable { self =>
  /**
   * Convert a value to JSON.
   */
  def apply(a: A): Json

  /**
   * Create a new [Encoder]] by applying a function to a value of type `B` before encoding as an
   * `A`.
   */
  final def contramap[B](f: B => A): Encoder[B] = new Encoder[B] {
    final def apply(a: B) = self(f(a))
  }

  /**
   * Create a new [[Encoder]] by applying a function to the output of this one.
   */
  final def mapJson(f: Json => Json): Encoder[A] = new Encoder[A] {
    final def apply(a: A): Json = f(self(a))
  }
}

/**
 * Utilities and instances for [[Encoder]].
 *
 * @groupname Utilities Miscellaneous utilities
 * @groupprio Utilities 0
 *
 * @groupname Encoding Encoder instances
 * @groupprio Encoding 1
 *
 * @groupname Disjunction Disjunction instances
 * @groupdesc Disjunction Instance creation methods for disjunction-like types. Note that these
 * instances are not implicit, since they require non-obvious decisions about the names of the
 * discriminators. If you want instances for these types you can include the following import in
 * your program:
 * {{{
 *   import io.circe.disjunctionCodecs._
 * }}}
 * @groupprio Disjunction 2
 *
 * @groupname Instances Type class instances
 * @groupprio Instances 3
 *
 * @groupname Tuple Tuple instances
 * @groupprio Tuple 4
 *
 * @groupname Product Case class and other product instances
 * @groupprio Product 5
 *
 * @author Travis Brown
 */
object Encoder extends TupleEncoders with ProductEncoders with MidPriorityEncoders {
  /**
   * Return an instance for a given type `A`.
   *
   * @group Utilities
   */
  final def apply[A](implicit instance: Encoder[A]): Encoder[A] = instance

  /**
   * Construct an instance from a function.
   *
   * @group Utilities
   */
  final def instance[A](f: A => Json): Encoder[A] = new Encoder[A] {
    final def apply(a: A): Json = f(a)
  }

  /**
   * Construct an instance for a given type with a [[cats.Foldable]] instance.
   *
   * @group Utilities
   */
  final def encodeFoldable[F[_], A](implicit e: Encoder[A], F: Foldable[F]): ArrayEncoder[F[A]] =
    new ArrayEncoder[F[A]] {
      final def encodeArray(a: F[A]): List[Json] =
        F.foldLeft(a, List.empty[Json])((list, v) => e(v) :: list).reverse
    }

  /**
   * @group Encoding
   */
  implicit final val encodeJson: Encoder[Json] = new Encoder[Json] {
    final def apply(a: Json): Json = a
  }

  /**
   * @group Encoding
   */
  implicit final val encodeJsonObject: ObjectEncoder[JsonObject] = new ObjectEncoder[JsonObject] {
    final def encodeObject(a: JsonObject): JsonObject = a
  }

  /**
   * @group Encoding
   */
  implicit final val encodeJsonNumber: Encoder[JsonNumber] = new Encoder[JsonNumber] {
    final def apply(a: JsonNumber): Json = Json.fromJsonNumber(a)
  }

  /**
   * @group Encoding
   */
  implicit final val encodeString: Encoder[String] = new Encoder[String] {
    final def apply(a: String): Json = Json.fromString(a)
  }

  /**
   * @group Encoding
   */
  implicit final val encodeUnit: Encoder[Unit] = new Encoder[Unit] {
    final def apply(a: Unit): Json = Json.fromJsonObject(JsonObject.empty)
  }

  /**
   * @group Encoding
   */
  implicit final val encodeBoolean: Encoder[Boolean] = new Encoder[Boolean] {
    final def apply(a: Boolean): Json = Json.fromBoolean(a)
  }

  /**
   * @group Encoding
   */
  implicit final val encodeChar: Encoder[Char] = new Encoder[Char] {
    final def apply(a: Char): Json = Json.fromString(a.toString)
  }

  /**
   * @group Encoding
   */
  implicit final val encodeFloat: Encoder[Float] = new Encoder[Float] {
    final def apply(a: Float): Json = Json.fromDoubleOrNull(a.toDouble)
  }

  /**
   * @group Encoding
   */
  implicit final val encodeDouble: Encoder[Double] = new Encoder[Double] {
    final def apply(a: Double): Json = Json.fromDoubleOrNull(a)
  }

  /**
   * @group Encoding
   */
  implicit final val encodeByte: Encoder[Byte] = new Encoder[Byte] {
    final def apply(a: Byte): Json = Json.fromInt(a.toInt)
  }

  /**
   * @group Encoding
   */
  implicit final val encodeShort: Encoder[Short] = new Encoder[Short] {
    final def apply(a: Short): Json = Json.fromInt(a.toInt)
  }

  /**
   * @group Encoding
   */
  implicit final val encodeInt: Encoder[Int] = new Encoder[Int] {
    final def apply(a: Int): Json = Json.fromInt(a)
  }

  /**
   * @group Encoding
   */
  implicit final val encodeLong: Encoder[Long] = new Encoder[Long] {
    final def apply(a: Long): Json = Json.fromLong(a)
  }

  /**
   * @group Encoding
   */
  implicit final val encodeBigInt: Encoder[BigInt] = new Encoder[BigInt] {
    final def apply(a: BigInt): Json = Json.fromBigInt(a)
  }

  /**
   * @group Encoding
   */
  implicit final val encodeBigDecimal: Encoder[BigDecimal] = new Encoder[BigDecimal] {
    final def apply(a: BigDecimal): Json = Json.fromBigDecimal(a)
  }

  /**
   * @group Encoding
   */
  implicit final val encodeUUID: Encoder[UUID] = new Encoder[UUID] {
    final def apply(a: UUID): Json = Json.fromString(a.toString)
  }

  /**
   * @group Encoding
   */
  implicit final def encodeOption[A](implicit e: Encoder[A]): Encoder[Option[A]] = new Encoder[Option[A]] {
    final def apply(a: Option[A]): Json = a match {
      case Some(v) => e(v)
      case None => Json.Null
    }
  }

  /**
   * @group Encoding
   */
  implicit final def encodeSome[A](implicit e: Encoder[A]): Encoder[Some[A]] = e.contramap(_.x)

  /**
   * @group Encoding
   */
  implicit final val encodeNone: Encoder[None.type] = new Encoder[None.type] {
    final def apply(a: None.type): Json = Json.Null
  }

  /**
   * @group Encoding
   */
  implicit final def encodeNonEmptyList[A](implicit e: Encoder[A]): Encoder[NonEmptyList[A]] =
    new ArrayEncoder[NonEmptyList[A]] {
      final def encodeArray(a: NonEmptyList[A]): List[Json] = a.toList.map(e(_))
    }

  /**
   * @group Encoding
   */
  implicit final def encodeNonEmptyVector[A](implicit e: Encoder[A]): Encoder[NonEmptyVector[A]] =
    new ArrayEncoder[NonEmptyVector[A]] {
      final def encodeArray(a: NonEmptyVector[A]): List[Json] = a.toVector.toList.map(e(_))
    }

  /**
   * @group Encoding
   */
  implicit final def encodeOneAnd[A0, C[_]](
    implicit ea: Encoder[A0],
    is: IsTraversableOnce[C[A0]] { type A = A0 }
  ): ArrayEncoder[OneAnd[C, A0]] = new ArrayEncoder[OneAnd[C, A0]] {
    private[this] val encoder = encodeTraversableOnce[A0, GenSeq]

    final def encodeArray(a: OneAnd[C, A0]): List[Json] = encoder.encodeArray(
      a.head :: is.conversion(a.tail).toList
    )
  }

  /**
   * @group Encoding
   */
  implicit final def encodeMapLike[M[K, +V] <: Map[K, V], K, V](implicit
    ek: KeyEncoder[K],
    ev: Encoder[V]
  ): ObjectEncoder[M[K, V]] = new ObjectEncoder[M[K, V]] {
    final def encodeObject(a: M[K, V]): JsonObject = JsonObject.fromMap(
      a.map {
        case (k, v) => (ek(k), ev(v))
      }
    )
  }

  /**
   * @group Disjunction
   */
  final def encodeEither[A, B](leftKey: String, rightKey: String)(implicit
    ea: Encoder[A],
    eb: Encoder[B]
  ): ObjectEncoder[Either[A, B]] = new ObjectEncoder[Either[A, B]] {
    final def encodeObject(a: Either[A, B]): JsonObject = a match {
      case Left(a) => JsonObject.singleton(leftKey, ea(a))
      case Right(b) => JsonObject.singleton(rightKey, eb(b))
    }
  }

  /**
   * @group Disjunction
   */
  final def encodeValidated[E, A](failureKey: String, successKey: String)(implicit
    ee: Encoder[E],
    ea: Encoder[A]
  ): ObjectEncoder[Validated[E, A]] = new ObjectEncoder[Validated[E, A]] {
    final def encodeObject(a: Validated[E, A]): JsonObject = a match {
      case Validated.Invalid(e) => JsonObject.singleton(failureKey, ee(e))
      case Validated.Valid(a) => JsonObject.singleton(successKey, ea(a))
    }
  }

  /**
   * @group Instances
   */
  implicit final val encoderContravariant: Contravariant[Encoder] = new Contravariant[Encoder] {
    final def contramap[A, B](e: Encoder[A])(f: B => A): Encoder[B] = e.contramap(f)
  }

  /**
    * @group Enumeration
    * {{{
    *   object WeekDay extends Enumeration { ... }
    *   implicit val weekDayEncoder = Encoder.enumEncoder(WeekDay)
    * }}}
    */
  final def enumEncoder[E <: Enumeration](enum: E): Encoder[E#Value] = new Encoder[E#Value] {
    override def apply(e: E#Value): Json = Encoder.encodeString(e.toString)
  }
}

private[circe] trait MidPriorityEncoders extends LowPriorityEncoders {
  /**
   * @group Encoding
   */
  implicit final def encodeTraversableOnce[A0, C[_]](implicit
    e: Encoder[A0],
    is: IsTraversableOnce[C[A0]] { type A = A0 }
  ): ArrayEncoder[C[A0]] = new ArrayEncoder[C[A0]] {
    final def encodeArray(a: C[A0]): List[Json] = {
      val items = List.newBuilder[Json]

      val it = is.conversion(a).toIterator

      while (it.hasNext) {
        items += e(it.next())
      }

      items.result
    }
  }
}

private[circe] trait LowPriorityEncoders {
  implicit final def importedEncoder[A](implicit exported: Exported[ObjectEncoder[A]]): Encoder[A] = exported.instance
}
