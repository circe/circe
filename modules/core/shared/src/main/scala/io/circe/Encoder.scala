package io.circe

import cats.{ Contravariant, Foldable }
import cats.data.{ NonEmptyList, NonEmptySet, NonEmptyVector, OneAnd, Validated }
import io.circe.export.Exported
import java.io.Serializable
import java.util.UUID
import scala.Predef._
import scala.collection.Map
import scala.collection.immutable.{ Map => ImmutableMap, Set }

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
   * Create a new [[Encoder]] by applying a function to a value of type `B` before encoding as an
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
 * @groupname Utilities Defining encoders
 * @groupprio Utilities 0
 *
 * @groupname Encoding General encoder instances
 * @groupprio Encoding 1
 *
 * @groupname Collection Collection instances
 * @groupprio Collection 2
 *
 * @groupname Disjunction Disjunction instances
 * @groupdesc Disjunction Instance creation methods for disjunction-like types. Note that these
 * instances are not implicit, since they require non-obvious decisions about the names of the
 * discriminators. If you want instances for these types you can include the following import in
 * your program:
 * {{{
 *   import io.circe.disjunctionCodecs._
 * }}}
 * @groupprio Disjunction 3
 *
 * @groupname Instances Type class instances
 * @groupprio Instances 4
 *
 * @groupname Tuple Tuple instances
 * @groupprio Tuple 5
 *
 * @groupname Product Case class and other product instances
 * @groupprio Product 6
 *
 * @groupname Prioritization Instance prioritization
 * @groupprio Prioritization 9
 *
 * @author Travis Brown
 */
final object Encoder extends TupleEncoders with ProductEncoders with JavaTimeEncoders with MidPriorityEncoders {
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
      final def encodeArray(a: F[A]): Vector[Json] =
        F.foldLeft(a, Vector.empty[Json])((list, v) => e(v) +: list).reverse
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
  implicit final val encodeUnit: ObjectEncoder[Unit] = new ObjectEncoder[Unit] {
    final def encodeObject(a: Unit): JsonObject = JsonObject.empty
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
  implicit final val encodeJavaBoolean: Encoder[java.lang.Boolean] = encodeBoolean.contramap(_.booleanValue())

  /**
   * @group Encoding
   */
  implicit final val encodeChar: Encoder[Char] = new Encoder[Char] {
    final def apply(a: Char): Json = Json.fromString(a.toString)
  }

  /**
    * @group Encoding
    */
  implicit final val encodeJavaCharacter: Encoder[java.lang.Character] = encodeChar.contramap(_.charValue())

  /**
   * Note that on Scala.js the encoding of `Float` values is subject to the
   * usual limitations of `Float#toString` on that platform (e.g. `1.1f` will be
   * encoded as a [[Json]] value that will be printed as `"1.100000023841858"`).
   *
   * @group Encoding
   */
  implicit final val encodeFloat: Encoder[Float] = new Encoder[Float] {
    final def apply(a: Float): Json = Json.fromFloatOrNull(a)
  }

  /**
    * @group Encoding
    */
  implicit final val encodeJavaFloat: Encoder[java.lang.Float] = encodeFloat.contramap(_.floatValue())

  /**
   * @group Encoding
   */
  implicit final val encodeDouble: Encoder[Double] = new Encoder[Double] {
    final def apply(a: Double): Json = Json.fromDoubleOrNull(a)
  }

  /**
    * @group Encoding
    */
  implicit final val encodeJavaDouble: Encoder[java.lang.Double] = encodeDouble.contramap(_.doubleValue())

  /**
   * @group Encoding
   */
  implicit final val encodeByte: Encoder[Byte] = new Encoder[Byte] {
    final def apply(a: Byte): Json = Json.fromInt(a.toInt)
  }

  /**
    * @group Encoding
    */
  implicit final val encodeJavaByte: Encoder[java.lang.Byte] = encodeByte.contramap(_.byteValue())

  /**
   * @group Encoding
   */
  implicit final val encodeShort: Encoder[Short] = new Encoder[Short] {
    final def apply(a: Short): Json = Json.fromInt(a.toInt)
  }

  /**
    * @group Encoding
    */
  implicit final val encodeJavaShort: Encoder[java.lang.Short] = encodeShort.contramap(_.shortValue())

  /**
   * @group Encoding
   */
  implicit final val encodeInt: Encoder[Int] = new Encoder[Int] {
    final def apply(a: Int): Json = Json.fromInt(a)
  }

  /**
    * @group Encoding
    */
  implicit final val encodeJavaInteger: Encoder[java.lang.Integer] = encodeInt.contramap(_.intValue())

  /**
   * @group Encoding
   */
  implicit final val encodeLong: Encoder[Long] = new Encoder[Long] {
    final def apply(a: Long): Json = Json.fromLong(a)
  }

  /**
    * @group Encoding
    */
  implicit final val encodeJavaLong: Encoder[java.lang.Long] = encodeLong.contramap(_.longValue())

  /**
   * @group Encoding
   */
  implicit final val encodeBigInt: Encoder[BigInt] = new Encoder[BigInt] {
    final def apply(a: BigInt): Json = Json.fromBigInt(a)
  }

  /**
    * @group Encoding
    */
  implicit final val encodeJavaBigInteger: Encoder[java.math.BigInteger] = encodeBigInt.contramap(BigInt.apply)

  /**
   * @group Encoding
   */
  implicit final val encodeBigDecimal: Encoder[BigDecimal] = new Encoder[BigDecimal] {
    final def apply(a: BigDecimal): Json = Json.fromBigDecimal(a)
  }

  /**
    * @group Encoding
    */
  implicit final val encodeJavaBigDecimal: Encoder[java.math.BigDecimal] = encodeBigDecimal.contramap(BigDecimal.apply)


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
  implicit final def encodeSome[A](implicit e: Encoder[A]): Encoder[Some[A]] = e.contramap(_.get)

  /**
   * @group Encoding
   */
  implicit final val encodeNone: Encoder[None.type] = new Encoder[None.type] {
    final def apply(a: None.type): Json = Json.Null
  }

  /**
   * @group Collection
   */
  implicit final def encodeSeq[A](implicit encodeA: Encoder[A]): ArrayEncoder[Seq[A]] =
    new IterableArrayEncoder[A, Seq](encodeA) {
      final protected def toIterator(a: Seq[A]): Iterator[A] = a.iterator
    }

  /**
   * @group Collection
   */
  implicit final def encodeSet[A](implicit encodeA: Encoder[A]): ArrayEncoder[Set[A]] =
    new IterableArrayEncoder[A, Set](encodeA) {
      final protected def toIterator(a: Set[A]): Iterator[A] = a.iterator
    }

  /**
   * @group Collection
   */
  implicit final def encodeList[A](implicit encodeA: Encoder[A]): ArrayEncoder[List[A]] =
    new IterableArrayEncoder[A, List](encodeA) {
      final protected def toIterator(a: List[A]): Iterator[A] = a.iterator
    }

  /**
   * @group Collection
   */
  implicit final def encodeVector[A](implicit encodeA: Encoder[A]): ArrayEncoder[Vector[A]] =
    new IterableArrayEncoder[A, Vector](encodeA) {
      final protected def toIterator(a: Vector[A]): Iterator[A] = a.iterator
    }

  /**
   * @group Collection
   */
  implicit final def encodeNonEmptyList[A](implicit encodeA: Encoder[A]): ArrayEncoder[NonEmptyList[A]] =
    new ArrayEncoder[NonEmptyList[A]] {
      final def encodeArray(a: NonEmptyList[A]): Vector[Json] = a.toList.toVector.map(encodeA(_))
    }

  /**
   * @group Collection
   */
  implicit final def encodeNonEmptyVector[A](implicit encodeA: Encoder[A]): ArrayEncoder[NonEmptyVector[A]] =
    new ArrayEncoder[NonEmptyVector[A]] {
      final def encodeArray(a: NonEmptyVector[A]): Vector[Json] = a.toVector.map(encodeA(_))
    }

  /**
    * @group Collection
    */
  implicit final def encodeNonEmptySet[A](implicit encodeA: Encoder[A]): ArrayEncoder[NonEmptySet[A]] =
    new ArrayEncoder[NonEmptySet[A]] {
      final def encodeArray(a: NonEmptySet[A]): Vector[Json] = a.toSortedSet.toVector.map(encodeA(_))
    }

  /**
   * @group Collection
   */
  implicit final def encodeOneAnd[A, C[_]](implicit
    encodeA: Encoder[A],
    ev: C[A] => Iterable[A]
  ): ArrayEncoder[OneAnd[C, A]] = new ArrayEncoder[OneAnd[C, A]] {
    private[this] val encoder: ArrayEncoder[Vector[A]] = encodeVector[A]

    final def encodeArray(a: OneAnd[C, A]): Vector[Json] = encoder.encodeArray(a.head +: ev(a.tail).toVector)
  }

  /**
   * @group Collection
   */
  implicit final def encodeMap[K, V](implicit
    encodeK: KeyEncoder[K],
    encodeV: Encoder[V]
  ): ObjectEncoder[ImmutableMap[K, V]] = new ObjectEncoder[ImmutableMap[K, V]] {
    final def encodeObject(a: ImmutableMap[K, V]): JsonObject = JsonObject.fromMap(
      a.map {
        case (k, v) => (encodeK(k), encodeV(v))
      }
    )
  }

  /**
   * @group Collection
   */
  implicit final def encodeMapLike[K, V, M[K, V] <: Map[K, V]](implicit
    encodeK: KeyEncoder[K],
    encodeV: Encoder[V],
    ev: M[K, V] => Iterable[(K, V)]
  ): ObjectEncoder[M[K, V]] = new IterableObjectEncoder[K, V, M](encodeK, encodeV) {
    final protected def toIterator(a: M[K, V]): Iterator[(K, V)] = ev(a).iterator
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
  ): ObjectEncoder[Validated[E, A]] = encodeEither[E, A](failureKey, successKey).contramapObject {
    case Validated.Invalid(e) => Left(e)
    case Validated.Valid(a) => Right(a)
  }

  /**
   * @group Instances
   */
  implicit final val encoderContravariant: Contravariant[Encoder] = new Contravariant[Encoder] {
    final def contramap[A, B](e: Encoder[A])(f: B => A): Encoder[B] = e.contramap(f)
  }

  /**
   * {{{
   *   object WeekDay extends Enumeration { ... }
   *   implicit val weekDayEncoder = Encoder.enumEncoder(WeekDay)
   * }}}
   * @group Utilities
   */
  final def enumEncoder[E <: Enumeration](enum: E): Encoder[E#Value] = new Encoder[E#Value] {
    override def apply(e: E#Value): Json = Encoder.encodeString(e.toString)
  }

  private[this] abstract class IterableObjectEncoder[K, V, M[_, _]](
    encodeK: KeyEncoder[K],
    encodeV: Encoder[V]
  ) extends ObjectEncoder[M[K, V]] {
    protected def toIterator(a: M[K, V]): Iterator[(K, V)]

    final def encodeObject(a: M[K, V]): JsonObject = {
      val builder = ImmutableMap.newBuilder[String, Json]
      val iterator = toIterator(a)

      while (iterator.hasNext) {
        val next = iterator.next()
        builder += ((encodeK(next._1), encodeV(next._2)))
      }

      JsonObject.fromMap(builder.result())
    }
  }
}

private[circe] trait MidPriorityEncoders extends LowPriorityEncoders {
  /**
   * @group Collection
   */
  implicit final def encodeIterable[A, C[_]](implicit
    encodeA: Encoder[A],
    ev: C[A] => Iterable[A]
  ): ArrayEncoder[C[A]] = new IterableArrayEncoder[A, C](encodeA) {
    final protected def toIterator(a: C[A]): Iterator[A] = ev(a).iterator
  }

  protected[this] abstract class IterableArrayEncoder[A, C[_]](encodeA: Encoder[A]) extends ArrayEncoder[C[A]] {
    protected def toIterator(a: C[A]): Iterator[A]

    final def encodeArray(a: C[A]): Vector[Json] = {
      val builder = Vector.newBuilder[Json]
      val iterator = toIterator(a)

      while (iterator.hasNext) {
        builder += encodeA(iterator.next())
      }

      builder.result()
    }
  }
}

private[circe] trait LowPriorityEncoders {
  /**
   * @group Prioritization
   */
  implicit final def importedEncoder[A](implicit exported: Exported[ObjectEncoder[A]]): Encoder[A] = exported.instance
}
