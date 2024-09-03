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

import cats.Contravariant
import cats.Defer
import cats.Foldable
import cats.data.Chain
import cats.data.NonEmptyChain
import cats.data.NonEmptyList
import cats.data.NonEmptyMap
import cats.data.NonEmptySet
import cats.data.NonEmptySeq
import cats.data.NonEmptyVector
import cats.data.OneAnd
import cats.data.Validated
import io.circe.`export`.Exported

import java.io.Serializable
import java.net.URI
import java.time.{
  Duration,
  Instant,
  LocalDate,
  LocalDateTime,
  LocalTime,
  MonthDay,
  OffsetDateTime,
  OffsetTime,
  Period,
  Year,
  YearMonth,
  ZoneId,
  ZoneOffset,
  ZonedDateTime
}
import java.time.format.{ DateTimeFormatter, DateTimeFormatterBuilder, SignStyle }
import java.time.format.DateTimeFormatter.{
  ISO_LOCAL_DATE_TIME,
  ISO_LOCAL_TIME,
  ISO_OFFSET_DATE_TIME,
  ISO_OFFSET_TIME,
  ISO_ZONED_DATE_TIME
}
import java.time.temporal.{ ChronoField, TemporalAccessor }
import java.util.Currency
import java.util.UUID
import scala.Predef._
import scala.collection.Map
import scala.collection.immutable.Set
import scala.collection.immutable.{ Map => ImmutableMap }

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
    final def apply(a: B): Json = self(f(a))
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
 * @groupprio Utilities 1
 *
 * @groupname Encoding General encoder instances
 * @groupprio Encoding 2
 *
 * @groupname Collection Collection instances
 * @groupprio Collection 3
 *
 * @groupname Disjunction Disjunction instances
 * @groupdesc Disjunction Instance creation methods for disjunction-like types. Note that these
 * instances are not implicit, since they require non-obvious decisions about the names of the
 * discriminators. If you want instances for these types you can include the following import in
 * your program:
 * {{{
 *   import io.circe.disjunctionCodecs._
 * }}}
 * @groupprio Disjunction 4
 *
 * @groupname Instances Type class instances
 * @groupprio Instances 5
 *
 * @groupname Tuple Tuple instances
 * @groupprio Tuple 6
 *
 * @groupname Product Case class and other product instances
 * @groupprio Product 7
 *
 * @groupname Time Java date and time instances
 * @groupprio Time 8
 *
 * @groupname Literal Literal type instances
 * @groupprio Literal 9
 *
 * @groupname Prioritization Instance prioritization
 * @groupprio Prioritization 10
 *
 * @author Travis Brown
 */
object Encoder
    extends TupleEncoders
    with ProductEncoders
    with ProductTypedEncoders
    with LiteralEncoders
    with EnumerationEncoders
    with MidPriorityEncoders
    with EncoderDerivationRelaxed {

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
   * Create an `Encoder` which assumes one already exists
   *
   * Certain recursive data structures (particularly when generic) greatly benefit from
   * being able to be written this way. See `cats.Defer`
   *
   * @group Utilities
   */
  final def recursive[A](fn: Encoder[A] => Encoder[A]): Encoder[A] = Defer[Encoder].fix(fn)

  /**
   * Construct an instance for a given type with a [[cats.Foldable]] instance.
   *
   * @group Utilities
   */
  final def encodeFoldable[F[_], A](implicit e: Encoder[A], F: Foldable[F]): AsArray[F[A]] =
    new AsArray[F[A]] {
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
  implicit final val encodeJsonObject: AsObject[JsonObject] = new AsObject[JsonObject] {
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
  implicit final val encodeUnit: AsObject[Unit] = new AsObject[Unit] {
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
  implicit final lazy val encodeJavaBoolean: Encoder[java.lang.Boolean] = encodeBoolean.contramap(_.booleanValue())

  /**
   * @group Encoding
   */
  implicit final val encodeChar: Encoder[Char] = new Encoder[Char] {
    final def apply(a: Char): Json = Json.fromString(a.toString)
  }

  /**
   * @group Encoding
   */
  implicit final lazy val encodeJavaCharacter: Encoder[java.lang.Character] = encodeChar.contramap(_.charValue())

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
  implicit final lazy val encodeJavaFloat: Encoder[java.lang.Float] = encodeFloat.contramap(_.floatValue())

  /**
   * @group Encoding
   */
  implicit final val encodeDouble: Encoder[Double] = new Encoder[Double] {
    final def apply(a: Double): Json = Json.fromDoubleOrNull(a)
  }

  /**
   * @group Encoding
   */
  implicit final lazy val encodeJavaDouble: Encoder[java.lang.Double] = encodeDouble.contramap(_.doubleValue())

  /**
   * @group Encoding
   */
  implicit final val encodeByte: Encoder[Byte] = new Encoder[Byte] {
    final def apply(a: Byte): Json = Json.fromInt(a.toInt)
  }

  /**
   * @group Encoding
   */
  implicit final lazy val encodeJavaByte: Encoder[java.lang.Byte] = encodeByte.contramap(_.byteValue())

  /**
   * @group Encoding
   */
  implicit final val encodeShort: Encoder[Short] = new Encoder[Short] {
    final def apply(a: Short): Json = Json.fromInt(a.toInt)
  }

  /**
   * @group Encoding
   */
  implicit final lazy val encodeJavaShort: Encoder[java.lang.Short] = encodeShort.contramap(_.shortValue())

  /**
   * @group Encoding
   */
  implicit final val encodeInt: Encoder[Int] = new Encoder[Int] {
    final def apply(a: Int): Json = Json.fromInt(a)
  }

  /**
   * @group Encoding
   */
  implicit final lazy val encodeJavaInteger: Encoder[java.lang.Integer] = encodeInt.contramap(_.intValue())

  /**
   * @group Encoding
   */
  implicit final val encodeLong: Encoder[Long] = new Encoder[Long] {
    final def apply(a: Long): Json = Json.fromLong(a)
  }

  /**
   * @group Encoding
   */
  implicit final lazy val encodeJavaLong: Encoder[java.lang.Long] = encodeLong.contramap(_.longValue())

  /**
   * @group Encoding
   */
  implicit final val encodeBigInt: Encoder[BigInt] = new Encoder[BigInt] {
    final def apply(a: BigInt): Json = Json.fromBigInt(a)
  }

  /**
   * @group Encoding
   */
  implicit final lazy val encodeJavaBigInteger: Encoder[java.math.BigInteger] = encodeBigInt.contramap(BigInt.apply)

  /**
   * @group Encoding
   */
  implicit final val encodeBigDecimal: Encoder[BigDecimal] = new Encoder[BigDecimal] {
    final def apply(a: BigDecimal): Json = Json.fromBigDecimal(a)
  }

  /**
   * @group Encoding
   */
  implicit final lazy val encodeJavaBigDecimal: Encoder[java.math.BigDecimal] =
    encodeBigDecimal.contramap(BigDecimal.apply)

  /**
   * @group Encoding
   */
  implicit final lazy val encodeUUID: Encoder[UUID] = new Encoder[UUID] {
    final def apply(a: UUID): Json = Json.fromString(a.toString)
  }

  /**
   * @group Encoding
   */
  implicit final lazy val encodeURI: Encoder[URI] = new Encoder[URI] {
    final def apply(a: URI): Json = Json.fromString(a.toString)
  }

  /**
   * @group Encoding
   */
  implicit final def encodeOption[A](implicit e: Encoder[A]): Encoder[Option[A]] = new Encoder[Option[A]] {
    final def apply(a: Option[A]): Json = a match {
      case Some(v) => e(v)
      case None    => Json.Null
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
   * @group Encoding
   */
  implicit final def encodeNullOr[A](implicit e: Encoder[A]): Encoder[NullOr[A]] = new Encoder[NullOr[A]] {
    final def apply(a: NullOr[A]): Json = {
      a match {
        case NullOr.Value(v) => e(v)
        case NullOr.Null     => Json.Null
      }
    }
  }

  /**
   * @group Encoding
   */
  implicit final def encodeNullOrValue[A](implicit e: Encoder[A]): Encoder[NullOr.Value[A]] = e.contramap(_.value)

  /**
   * @group Encoding
   */
  implicit final def encodeNullOrNull: Encoder[NullOr.Null.type] =
    new Encoder[NullOr.Null.type] {
      final def apply(a: NullOr.Null.type): Json = Json.Null
    }

  /**
   * @group Collection
   */
  implicit final def encodeSeq[A](implicit encodeA: Encoder[A]): AsArray[Seq[A]] =
    new IterableAsArrayEncoder[A, Seq](encodeA) {
      final protected def toIterator(a: Seq[A]): Iterator[A] = a.iterator
    }

  /**
   * @group Collection
   */
  implicit final def encodeSet[A](implicit encodeA: Encoder[A]): AsArray[Set[A]] =
    new IterableAsArrayEncoder[A, Set](encodeA) {
      final protected def toIterator(a: Set[A]): Iterator[A] = a.iterator
    }

  /**
   * @group Collection
   */
  implicit final def encodeList[A](implicit encodeA: Encoder[A]): AsArray[List[A]] =
    new IterableAsArrayEncoder[A, List](encodeA) {
      final protected def toIterator(a: List[A]): Iterator[A] = a.iterator
    }

  /**
   * @group Collection
   */
  implicit final def encodeVector[A](implicit encodeA: Encoder[A]): AsArray[Vector[A]] =
    new IterableAsArrayEncoder[A, Vector](encodeA) {
      final protected def toIterator(a: Vector[A]): Iterator[A] = a.iterator
    }

  /**
   * @group Collection
   */
  implicit final def encodeChain[A](implicit encodeA: Encoder[A]): AsArray[Chain[A]] =
    new IterableAsArrayEncoder[A, Chain](encodeA) {
      final protected def toIterator(a: Chain[A]): Iterator[A] = a.iterator
    }

  /**
   * @group Collection
   */
  implicit final def encodeNonEmptyList[A](implicit encodeA: Encoder[A]): AsArray[NonEmptyList[A]] =
    new AsArray[NonEmptyList[A]] {
      final def encodeArray(a: NonEmptyList[A]): Vector[Json] = a.toList.toVector.map(encodeA(_))
    }

  /**
   * @group Collection
   */
  implicit final def encodeNonEmptySeq[A](implicit encodeA: Encoder[A]): AsArray[NonEmptySeq[A]] =
    new AsArray[NonEmptySeq[A]] {
      final def encodeArray(a: NonEmptySeq[A]): Vector[Json] = a.toSeq.toVector.map(encodeA(_))
    }

  /**
   * @group Collection
   */
  implicit final def encodeNonEmptyVector[A](implicit encodeA: Encoder[A]): AsArray[NonEmptyVector[A]] =
    new AsArray[NonEmptyVector[A]] {
      final def encodeArray(a: NonEmptyVector[A]): Vector[Json] = a.toVector.map(encodeA(_))
    }

  /**
   * @group Collection
   */
  implicit final def encodeNonEmptySet[A](implicit encodeA: Encoder[A]): AsArray[NonEmptySet[A]] =
    new AsArray[NonEmptySet[A]] {
      final def encodeArray(a: NonEmptySet[A]): Vector[Json] = a.toSortedSet.toVector.map(encodeA(_))
    }

  /**
   * @group Collection
   */
  implicit final def encodeNonEmptyMap[K, V](implicit
    encodeK: KeyEncoder[K],
    encodeV: Encoder[V]
  ): AsObject[NonEmptyMap[K, V]] =
    new AsObject[NonEmptyMap[K, V]] {
      final def encodeObject(a: NonEmptyMap[K, V]): JsonObject =
        encodeMap[K, V].encodeObject(a.toSortedMap)
    }

  /**
   * @group Collection
   */
  implicit final def encodeNonEmptyChain[A](implicit encodeA: Encoder[A]): AsArray[NonEmptyChain[A]] =
    new AsArray[NonEmptyChain[A]] {
      final def encodeArray(a: NonEmptyChain[A]): Vector[Json] = a.toChain.toVector.map(encodeA(_))
    }

  /**
   * @group Collection
   */
  implicit final def encodeOneAnd[A, C[_]](implicit
    encodeA: Encoder[A],
    ev: C[A] => Iterable[A]
  ): AsArray[OneAnd[C, A]] = new AsArray[OneAnd[C, A]] {
    private[this] val encoder: AsArray[Vector[A]] = encodeVector[A]

    final def encodeArray(a: OneAnd[C, A]): Vector[Json] = encoder.encodeArray(a.head +: ev(a.tail).toVector)
  }

  /**
   * Preserves iteration order.
   *
   * @group Collection
   */
  implicit final def encodeMap[K, V](implicit
    encodeK: KeyEncoder[K],
    encodeV: Encoder[V]
  ): AsObject[ImmutableMap[K, V]] =
    encodeMapLike[K, V, ImmutableMap](encodeK, encodeV, identity)

  /**
   * Preserves iteration order.
   *
   * @group Collection
   */
  implicit final def encodeMapLike[K, V, M[K, V] <: Map[K, V]](implicit
    encodeK: KeyEncoder[K],
    encodeV: Encoder[V],
    ev: M[K, V] => Iterable[(K, V)]
  ): AsObject[M[K, V]] = new IterableAsObjectEncoder[K, V, M](encodeK, encodeV) {
    final protected def toIterator(a: M[K, V]): Iterator[(K, V)] = ev(a).iterator
  }

  /**
   * @group Disjunction
   */
  final def encodeEither[A, B](leftKey: String, rightKey: String)(implicit
    encodeA: Encoder[A],
    encodeB: Encoder[B]
  ): AsObject[Either[A, B]] = new AsObject[Either[A, B]] {
    final def encodeObject(a: Either[A, B]): JsonObject = a match {
      case Left(a)  => JsonObject.singleton(leftKey, encodeA(a))
      case Right(b) => JsonObject.singleton(rightKey, encodeB(b))
    }
  }

  /**
   * @group Disjunction
   */
  final def encodeValidated[E, A](failureKey: String, successKey: String)(implicit
    encodeE: Encoder[E],
    encodeA: Encoder[A]
  ): AsObject[Validated[E, A]] = encodeEither[E, A](failureKey, successKey).contramapObject {
    case Validated.Invalid(e) => Left(e)
    case Validated.Valid(a)   => Right(a)
  }

  /**
   * @group Instances
   */
  implicit final val encoderContravariant: Contravariant[Encoder] = new Contravariant[Encoder] {
    final def contramap[A, B](e: Encoder[A])(f: B => A): Encoder[B] = e.contramap(f)
  }

  /**
   * Note that this implementation assumes that the collection does not contain duplicate keys.
   */
  private[this] abstract class IterableAsObjectEncoder[K, V, M[_, _]](
    encodeK: KeyEncoder[K],
    encodeV: Encoder[V]
  ) extends AsObject[M[K, V]] {
    protected def toIterator(a: M[K, V]): Iterator[(K, V)]

    final def encodeObject(a: M[K, V]): JsonObject = {
      val builder = ImmutableMap.newBuilder[String, Json]
      val keysBuilder = Vector.newBuilder[String]
      val iterator = toIterator(a)

      while (iterator.hasNext) {
        val next = iterator.next()
        val key = encodeK(next._1)
        builder += ((key, encodeV(next._2)))
        keysBuilder += key
      }

      JsonObject.fromMapAndVector(builder.result(), keysBuilder.result())
    }
  }

  private[this] abstract class JavaTimeEncoder[A <: TemporalAccessor] extends Encoder[A] {
    protected[this] def format: DateTimeFormatter

    final def apply(a: A): Json = Json.fromString(format.format(a))
  }

  /**
   * @group Time
   */
  implicit final lazy val encodeDuration: Encoder[Duration] = new Encoder[Duration] {
    final def apply(a: Duration): Json = Json.fromString(a.toString)
  }

  /**
   * @group Time
   */
  implicit final lazy val encodeInstant: Encoder[Instant] = new Encoder[Instant] {
    final def apply(a: Instant): Json = Json.fromString(a.toString)
  }

  /**
   * @group Time
   */
  implicit final lazy val encodePeriod: Encoder[Period] = new Encoder[Period] {
    final def apply(a: Period): Json = Json.fromString(a.toString)
  }

  /**
   * @group Time
   */
  implicit final lazy val encodeZoneId: Encoder[ZoneId] = new Encoder[ZoneId] {
    final def apply(a: ZoneId): Json = Json.fromString(a.getId)
  }

  /**
   * @group Time
   */
  final def encodeLocalDateWithFormatter(formatter: DateTimeFormatter): Encoder[LocalDate] =
    new JavaTimeEncoder[LocalDate] {
      protected[this] final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  final def encodeLocalTimeWithFormatter(formatter: DateTimeFormatter): Encoder[LocalTime] =
    new JavaTimeEncoder[LocalTime] {
      protected[this] final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  final def encodeLocalDateTimeWithFormatter(formatter: DateTimeFormatter): Encoder[LocalDateTime] =
    new JavaTimeEncoder[LocalDateTime] {
      protected[this] final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  final def encodeMonthDayWithFormatter(formatter: DateTimeFormatter): Encoder[MonthDay] =
    new JavaTimeEncoder[MonthDay] {
      protected[this] final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  final def encodeOffsetTimeWithFormatter(formatter: DateTimeFormatter): Encoder[OffsetTime] =
    new JavaTimeEncoder[OffsetTime] {
      protected[this] final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  final def encodeOffsetDateTimeWithFormatter(formatter: DateTimeFormatter): Encoder[OffsetDateTime] =
    new JavaTimeEncoder[OffsetDateTime] {
      protected[this] final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  final def encodeYearWithFormatter(formatter: DateTimeFormatter): Encoder[Year] =
    new JavaTimeEncoder[Year] {
      protected[this] final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  final def encodeYearMonthWithFormatter(formatter: DateTimeFormatter): Encoder[YearMonth] =
    new JavaTimeEncoder[YearMonth] {
      protected[this] final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  final def encodeZonedDateTimeWithFormatter(formatter: DateTimeFormatter): Encoder[ZonedDateTime] =
    new JavaTimeEncoder[ZonedDateTime] {
      protected[this] final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  final def encodeZoneOffsetWithFormatter(formatter: DateTimeFormatter): Encoder[ZoneOffset] =
    new JavaTimeEncoder[ZoneOffset] {
      protected[this] final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  implicit final lazy val encodeLocalDate: Encoder[LocalDate] = new Encoder[LocalDate] {
    final def apply(a: LocalDate): Json = Json.fromString(a.toString)
  }

  /**
   * @group Time
   */
  implicit final lazy val encodeLocalTime: Encoder[LocalTime] =
    new JavaTimeEncoder[LocalTime] {
      protected[this] final def format: DateTimeFormatter = ISO_LOCAL_TIME
    }

  /**
   * @group Time
   */
  implicit final lazy val encodeLocalDateTime: Encoder[LocalDateTime] =
    new JavaTimeEncoder[LocalDateTime] {
      protected[this] final def format: DateTimeFormatter = ISO_LOCAL_DATE_TIME
    }

  /**
   * @group Time
   */
  implicit final lazy val encodeMonthDay: Encoder[MonthDay] = new Encoder[MonthDay] {
    final def apply(a: MonthDay): Json = Json.fromString(a.toString)
  }

  /**
   * @group Time
   */
  implicit final lazy val encodeOffsetTime: Encoder[OffsetTime] =
    new JavaTimeEncoder[OffsetTime] {
      protected final def format: DateTimeFormatter = ISO_OFFSET_TIME
    }

  /**
   * @group Time
   */
  implicit final lazy val encodeOffsetDateTime: Encoder[OffsetDateTime] =
    new JavaTimeEncoder[OffsetDateTime] {
      protected final def format: DateTimeFormatter = ISO_OFFSET_DATE_TIME
    }

  /**
   * @group Time
   */
  implicit final lazy val encodeYear: Encoder[Year] =
    new JavaTimeEncoder[Year] {
      def format =
        new DateTimeFormatterBuilder().appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD).toFormatter()
    }

  /**
   * @group Time
   */
  implicit final lazy val encodeYearMonth: Encoder[YearMonth] =
    new JavaTimeEncoder[YearMonth] {
      def format = new DateTimeFormatterBuilder()
        .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
        .appendLiteral('-')
        .appendValue(ChronoField.MONTH_OF_YEAR, 2)
        .toFormatter()
    }

  /**
   * @group Time
   */
  implicit final lazy val encodeZonedDateTime: Encoder[ZonedDateTime] =
    new JavaTimeEncoder[ZonedDateTime] {
      protected final def format: DateTimeFormatter = ISO_ZONED_DATE_TIME
    }

  /**
   * @group Time
   */
  implicit final lazy val encodeZoneOffset: Encoder[ZoneOffset] = new Encoder[ZoneOffset] {
    final def apply(a: ZoneOffset): Json = Json.fromString(a.toString)
  }

  implicit final lazy val currencyEncoder: Encoder[Currency] =
    Encoder[String].contramap(_.getCurrencyCode())

  private case class DeferredEncoder[A](encoder: () => Encoder[A]) extends Encoder[A] {
    private lazy val resolved: Encoder[A] = resolve(encoder)

    @annotation.tailrec
    private def resolve(f: () => Encoder[A]): Encoder[A] =
      f() match {
        case DeferredEncoder(f) => resolve(f)
        case next               => next
      }

    override def apply(a: A): Json = resolved(a)
  }

  implicit val encoderInstances: Defer[Encoder] = new Defer[Encoder] {
    override def defer[A](fa: => Encoder[A]): Encoder[A] = DeferredEncoder(() => fa)
  }

  /**
   * A subtype of `Encoder` that statically verifies that the instance encodes
   * either a JSON array or an object.
   *
   * @author Travis Brown
   */
  trait AsRoot[A] extends Encoder[A]

  /**
   * Utilities and instances for [[AsRoot]].
   *
   * @groupname Utilities Defining encoders
   * @groupprio Utilities 1
   *
   * @groupname Prioritization Instance prioritization
   * @groupprio Prioritization 2
   *
   * @author Travis Brown
   */
  object AsRoot extends LowPriorityAsRootEncoders {

    /**
     * Return an instance for a given type.
     *
     * @group Utilities
     */
    final def apply[A](implicit instance: AsRoot[A]): AsRoot[A] = instance
  }

  private[circe] class LowPriorityAsRootEncoders {

    /**
     * @group Prioritization
     */
    implicit final def importedAsRootEncoder[A](implicit exported: Exported[AsRoot[A]]): AsRoot[A] =
      exported.instance
  }

  /**
   * A type class that provides a conversion from a value of type `A` to a JSON
   * array.
   *
   * @author Travis Brown
   */
  trait AsArray[A] extends AsRoot[A] { self =>
    final def apply(a: A): Json = Json.fromValues(encodeArray(a))

    /**
     * Convert a value to a JSON array.
     */
    def encodeArray(a: A): Vector[Json]

    /**
     * Create a new [[AsArray]] by applying a function to a value of type `B` before encoding as
     * an `A`.
     */
    final def contramapArray[B](f: B => A): AsArray[B] = new AsArray[B] {
      final def encodeArray(a: B): Vector[Json] = self.encodeArray(f(a))
    }

    /**
     * Create a new [[AsArray]] by applying a function to the output of this
     * one.
     */
    final def mapJsonArray(f: Vector[Json] => Vector[Json]): AsArray[A] = new AsArray[A] {
      final def encodeArray(a: A): Vector[Json] = f(self.encodeArray(a))
    }
  }

  /**
   * Utilities and instances for [[AsArray]].
   *
   * @groupname Utilities Defining encoders
   * @groupprio Utilities 1
   *
   * @groupname Instances Type class instances
   * @groupprio Instances 2
   *
   * @groupname Prioritization Instance prioritization
   * @groupprio Prioritization 3
   *
   * @author Travis Brown
   */
  object AsArray extends LowPriorityAsArrayEncoders {

    /**
     * Return an instance for a given type.
     *
     * @group Utilities
     */
    final def apply[A](implicit instance: AsArray[A]): AsArray[A] = instance

    /**
     * Construct an instance from a function.
     *
     * @group Utilities
     */
    final def instance[A](f: A => Vector[Json]): AsArray[A] = new AsArray[A] {
      final def encodeArray(a: A): Vector[Json] = f(a)
    }

    /**
     * @group Instances
     */
    implicit final val arrayEncoderContravariant: Contravariant[AsArray] = new Contravariant[AsArray] {
      final def contramap[A, B](e: AsArray[A])(f: B => A): AsArray[B] = e.contramapArray(f)
    }
  }

  private[circe] class LowPriorityAsArrayEncoders {

    /**
     * @group Prioritization
     */
    implicit final def importedAsArrayEncoder[A](implicit exported: Exported[AsArray[A]]): AsArray[A] =
      exported.instance
  }

  /**
   * A type class that provides a conversion from a value of type `A` to a
   * [[JsonObject]].
   *
   * @author Travis Brown
   */
  trait AsObject[A] extends AsRoot[A] { self =>
    final def apply(a: A): Json = Json.fromJsonObject(encodeObject(a))

    /**
     * Convert a value to a JSON object.
     */
    def encodeObject(a: A): JsonObject

    /**
     * Create a new [[Encoder.AsObject]] by applying a function to a value of type `B` before encoding as an
     * `A`.
     */
    final def contramapObject[B](f: B => A): AsObject[B] = new AsObject[B] {
      final def encodeObject(a: B): JsonObject = self.encodeObject(f(a))
    }

    /**
     * Create a new [[Encoder.AsObject]] by applying a function to the output of this
     * one.
     */
    final def mapJsonObject(f: JsonObject => JsonObject): AsObject[A] = new AsObject[A] {
      final def encodeObject(a: A): JsonObject = f(self.encodeObject(a))
    }
  }

  /**
   * Utilities and instances for [[Encoder.AsObject]].
   *
   * @groupname Utilities Defining encoders
   * @groupprio Utilities 1
   *
   * @groupname Instances Type class instances
   * @groupprio Instances 2
   *
   * @groupname Prioritization Instance prioritization
   * @groupprio Prioritization 3
   *
   * @author Travis Brown
   */
  object AsObject extends LowPriorityAsObjectEncoders with EncoderDerivation {

    /**
     * Return an instance for a given type.
     *
     * @group Utilities
     */
    final def apply[A](implicit instance: AsObject[A]): AsObject[A] = instance

    /**
     * Construct an instance from a function.
     *
     * @group Utilities
     */
    final def instance[A](f: A => JsonObject): AsObject[A] = new AsObject[A] {
      final def encodeObject(a: A): JsonObject = f(a)
    }

    /**
     * @group Instances
     */
    implicit final val objectEncoderContravariant: Contravariant[AsObject] = new Contravariant[AsObject] {
      final def contramap[A, B](e: AsObject[A])(f: B => A): AsObject[B] = e.contramapObject(f)
    }
  }

  private[circe] class LowPriorityAsObjectEncoders {

    /**
     * @group Prioritization
     */
    implicit final def importedAsObjectEncoder[A](implicit
      exported: Exported[AsObject[A]]
    ): AsObject[A] = exported.instance
  }
}

private[circe] trait MidPriorityEncoders extends LowPriorityEncoders {

  /**
   * @group Collection
   */
  implicit final def encodeIterable[A, C[_]](implicit
    encodeA: Encoder[A],
    ev: C[A] => Iterable[A]
  ): Encoder.AsArray[C[A]] = new IterableAsArrayEncoder[A, C](encodeA) {
    final protected def toIterator(a: C[A]): Iterator[A] = ev(a).iterator
  }

  protected[this] abstract class IterableAsArrayEncoder[A, C[_]](encodeA: Encoder[A]) extends Encoder.AsArray[C[A]] {
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
  implicit final def importedEncoder[A](implicit exported: Exported[Encoder[A]]): Encoder[A] = exported.instance
}
