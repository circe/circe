package io.circe

import cats.{ Contravariant, Foldable }
import cats.data.{ Chain, NonEmptyChain, NonEmptyList, NonEmptyMap, NonEmptySet, NonEmptyVector, OneAnd, Validated }
import io.circe.`export`.Exported
import java.io.Serializable
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
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.{
  ISO_LOCAL_DATE_TIME,
  ISO_LOCAL_TIME,
  ISO_OFFSET_DATE_TIME,
  ISO_OFFSET_TIME,
  ISO_ZONED_DATE_TIME
}
import java.time.temporal.TemporalAccessor
import java.util.UUID
import scala.Predef._
import scala.collection.Map
import scala.collection.immutable.{ Map => ImmutableMap, Set }

/**
 * A type class that provides a conversion from a value of type `A` to a [[Json]] value.
 *
 * @tparam A the type of value that we are encoding into a Json-like representation. 
 * @tparam J The data type in which the Json is represented.
 * @author Travis Brown
 */
trait Encoder[A, J] extends Serializable { self =>

  /**
   * Convert a value to JSON.
   */
  def apply(a: A): J

  /**
   * Create a new [[Encoder]] by applying a function to a value of type `B` before encoding as an
   * `A`.
   */
  final def contramap[B](f: B => A): Encoder[B, J] = new Encoder[B, J] {
    final def apply(a: B): TargetJson = self(f(a))
  }

  /**
   * Create a new [[Encoder]] by applying a function to the output of this one.
   */
  final def map[K](f: J => K): Encoder[A, K] = new Encoder[A, K] {
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
    with LiteralEncoders
    with EnumerationEncoders
    with MidPriorityEncoders {

  /**
   * Return an instance for a given type `A`.
   *
   * @group Utilities
   */
  final def apply[A, J](implicit instance: Encoder[A, J]): Encoder[A, J] = instance

  /**
   * Construct an instance from a function.
   *
   * @group Utilities
   */
  final def instance[A, J](f: A => J): Encoder[A, J] = new Encoder[A, J] {
    final def apply(a: A): J = f(a)
  }

  /**
   * Construct an instance for a given type with a [[cats.Foldable]] instance.
   *
   * @group Utilities
   */
  final def encodeFoldable[F[_], A, J](implicit e: Encoder[A, J], F: Foldable[F]): AsArray[F[A]] =
    new AsArray[F[A]] {
      final def encodeArray(a: F[A]): Vector[Json] =
        F.foldLeft(a, Vector.empty[Json])((list, v) => e(v) +: list).reverse
    }

  /**
   * @group Encoding
   */
  implicit final val encodeJson: Encoder[Json, Json] = new Encoder[Json, Json] {
    final def apply(a: Json): Json = a
  }

  /**
   * @group Encoding
   */
  implicit final def encodeJsonObject[J]: AsObject[JsonObject[J]] = new AsObject[JsonObject, J] {
    final def encodeObject(a: JsonObject[J]): JsonObject[J] = a
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
  implicit final def encodeUnit[J]: AsObject[Unit, J] = new AsObject[Unit, J] {
    final def encodeObject(a: Unit): JsonObject[J] = JsonObject.empty[J]
  }

  /**
   * @group Encoding
   */
  implicit final def encodeBoolean[J: JsonFactory]: Encoder[Boolean, J] = new Encoder[Boolean, J] {
    final def apply(a: Boolean): J = JsonFactory[J].fromBoolean(a)
  }

  /**
   * @group Encoding
   */
  implicit final def encodeJavaBoolean: Encoder[java.lang.Boolean, J] = encodeBoolean.contramap(_.booleanValue())

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
  implicit final def encodeLong[J: JsonFactory]: Encoder[Long, J] = new Encoder[Long, J] {
    final def apply(a: Long): J = JsonFactory[J].fromLong(a)
  }

  /**
   * @group Encoding
   */
  implicit final def encodeJavaLong[J: JsonFactory]: Encoder[java.lang.Long, J] =
    encodeLong.contramap(_.longValue())

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
  implicit final def encodeUUID[J: JsonFactory]: Encoder[UUID, J] = new Encoder[UUID, J] {
    final def apply(a: UUID): Json = Json.fromString(a.toString)
  }

  /**
   * @group Encoding
   */
  implicit final def encodeOption[A, J: JsonFactory](implicit e: Encoder[A, J]): Encoder[Option[A], J] = new Encoder[Option[A], J] {
    final def apply(a: Option[A]): J = a match {
      case Some(v) => e(v)
      case None    => JsonFactory[J].Null
    }
  }

  /**
   * @group Encoding
   */
  implicit final def encodeSome[A, J](implicit e: Encoder[A, J]): Encoder[Some[A], J] = e.contramap(_.get)

  /**
   * @group Encoding
   */
  implicit final def encodeNone[J: JsonFactory[J]]: Encoder[None.type, J] = new Encoder[None.type, J] {
    final def apply(a: None.type): J = JsonFactory[J].Null
  }

  /**
   * @group Collection
   */
  implicit final def encodeSeq[A, J](implicit encodeA: Encoder[A, J]): AsArray[Seq[A], J] =
    new IterableAsArrayEncoder[A, Seq, J](encodeA) {
      final protected def toIterator(a: Seq[A]): Iterator[A] = a.iterator
    }

  /**
   * @group Collection
   */
  implicit final def encodeSet[A, J](implicit encodeA: Encoder[A, J]): AsArray[Set[A], J] =
    new IterableAsArrayEncoder[A, Set, J](encodeA) {
      final protected def toIterator(a: Set[A]): Iterator[A] = a.iterator
    }

  /**
   * @group Collection
   */
  implicit final def encodeList[A, J](implicit encodeA: Encoder[A, J]): AsArray[List[A], J] =
    new IterableAsArrayEncoder[A, List, J](encodeA) {
      final protected def toIterator(a: List[A]): Iterator[A] = a.iterator
    }

  /**
   * @group Collection
   */
  implicit final def encodeVector[A, J](implicit encodeA: Encoder[A, J]): AsArray[Vector[A], J] =
    new IterableAsArrayEncoder[A, Vector, J](encodeA) {
      final protected def toIterator(a: Vector[A]): Iterator[A] = a.iterator
    }

  /**
   * @group Collection
   */
  implicit final def encodeChain[A, J](implicit encodeA: Encoder[A, J]): AsArray[Chain[A], J] =
    new IterableAsArrayEncoder[A, Chain, J](encodeA) {
      final protected def toIterator(a: Chain[A]): Iterator[A] = a.iterator
    }

  /**
   * @group Collection
   */
  implicit final def encodeNonEmptyList[A, J](implicit encodeA: Encoder[A, J]): AsArray[NonEmptyList[A], J] =
    new AsArray[NonEmptyList[A], J] {
      final def encodeArray(a: NonEmptyList[A]): Vector[J] = a.toList.toVector.map(encodeA(_))
    }

  /**
   * @group Collection
   */
  implicit final def encodeNonEmptyVector[A, J](implicit encodeA: Encoder[A, J]): AsArray[NonEmptyVector[A], J] =
    new AsArray[NonEmptyVector[A], J] {
      final def encodeArray(a: NonEmptyVector[A]): Vector[J] = a.toVector.map(encodeA(_))
    }

  /**
   * @group Collection
   */
  implicit final def encodeNonEmptySet[A, J](implicit encodeA: Encoder[A, J]): AsArray[NonEmptySet[A], J] =
    new AsArray[NonEmptySet[A], J] {
      final def encodeArray(a: NonEmptySet[A]): Vector[J] = a.toSortedSet.toVector.map(encodeA(_))
    }

  /**
   * @group Collection
   */
  implicit final def encodeNonEmptyMap[K, V, J](implicit
    encodeK: KeyEncoder[K],
    encodeV: Encoder[V, J]
  ): AsObject[NonEmptyMap[K, V]] =
    new AsObject[NonEmptyMap[K, V], J] {
      final def encodeObject(a: NonEmptyMap[K, V]): JsonObject[J] =
        encodeMap[K, V].encodeObject(a.toSortedMap)
    }

  /**
   * @group Collection
   */
  implicit final def encodeNonEmptyChain[A, J](implicit encodeA: Encoder[A, J]): AsArray[NonEmptyChain[A], J] =
    new AsArray[NonEmptyChain[A], J] {
      final def encodeArray(a: NonEmptyChain[A]): Vector[J] = a.toChain.toVector.map(encodeA(_))
    }

  /**
   * @group Collection
   */
  implicit final def encodeOneAnd[A, C[_], J](implicit
    encodeA: Encoder[A, J],
    ev: C[A] => Iterable[A]
  ): AsArray[OneAnd[C, A], J] = new AsArray[OneAnd[C, A], J] {
    private[this] val encoder: AsArray[Vector[A], J] = encodeVector[A]

    final def encodeArray(a: OneAnd[C, A]): Vector[J] = encoder.encodeArray(a.head +: ev(a.tail).toVector)
  }

  /**
   * Preserves iteration order.
   *
   * @group Collection
   */
  implicit final def encodeMap[K, V, J](implicit
    encodeK: KeyEncoder[K],
    encodeV: Encoder[V, J]
  ): AsObject[ImmutableMap[K, V]] =
    encodeMapLike[K, V, ImmutableMap](encodeK, encodeV, identity)

  /**
   * Preserves iteration order.
   *
   * @group Collection
   */
  implicit final def encodeMapLike[K, V, M[K, V] <: Map[K, V], J](implicit
    encodeK: KeyEncoder[K],
    encodeV: Encoder[V, J],
    ev: M[K, V] => Iterable[(K, V)]
  ): AsObject[M[K, V]] = new IterableAsObjectEncoder[K, V, M](encodeK, encodeV) {
    final protected def toIterator(a: M[K, V]): Iterator[(K, V)] = ev(a).iterator
  }

  /**
   * @group Disjunction
   */
  final def encodeEither[A, B](leftKey: String, rightKey: String)(implicit
    encodeA: Encoder[A, J],
    encodeB: Encoder[B, J]
  ): AsObject[Either[A, B], J] = new AsObject[Either[A, B], J] {
    final def encodeObject(a: Either[A, B]): JsonObject[J] = a match {
      case Left(a)  => JsonObject.singleton(leftKey, encodeA(a))
      case Right(b) => JsonObject.singleton(rightKey, encodeB(b))
    }
  }

  /**
   * @group Disjunction
   */
  final def encodeValidated[E, A, J](failureKey: String, successKey: String)(implicit
    encodeE: Encoder[E, J],
    encodeA: Encoder[A, J]
  ): AsObject[Validated[E, A], J] = encodeEither[E, A](failureKey, successKey).contramapObject {
    case Validated.Invalid(e) => Left(e)
    case Validated.Valid(a)   => Right(a)
  }

  /**
   * @group Instances
   */
  implicit final def encoderContravariant[J]: Contravariant[Encoder[?, J]] = new Contravariant[Encoder[?, J]] {
    final def contramap[A, B](e: Encoder[A, J])(f: B => A): Encoder[B, J] = e.contramap(f)
  }

  /**
   * Note that this implementation assumes that the collection does not contain duplicate keys.
   */
  private[this] abstract class IterableAsObjectEncoder[K, V, M[_, _], J](
    encodeK: KeyEncoder[K],
    encodeV: Encoder[V, J]
  ) extends AsObject[M[K, V]] {
    protected def toIterator(a: M[K, V]): Iterator[(K, V)]

    final def encodeObject(a: M[K, V]): JsonObject[J] = {
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

  private[this] abstract class JavaTimeEncoder[A <: TemporalAccessor, J: JsonFactory] extends Encoder[A, J] {
    protected[this] def format: DateTimeFormatter

    final def apply(a: A): J = JsonFactory[J].fromString(format.format(a))
  }

  /**
   * @group Time
   */
  implicit final def encodeDuration[J](implicit J: JsonFactory[J]): Encoder[Duration, J] = new Encoder[Duration, J] {
    final def apply(a: Duration): J = J.fromString(a.toString)
  }

  /**
   * @group Time
   */
  implicit final def encodeInstant[J: JsonFactory]: Encoder[Instant, J] = new Encoder[Instant, J] {
    final def apply(a: Instant): J = J.fromString(a.toString)
  }

  /**
   * @group Time
   */
  implicit final def encodePeriod[J](implicit J: JsonFactory[J]): Encoder[Period, J] = new Encoder[Period, J] {
    final def apply(a: Period): J = J.fromString(a.toString)
  }

  /**
   * @group Time
   */
  implicit final def encodeZoneId[J](implicit J: JsonFactory[J]): Encoder[ZoneId, J] =
    new Encoder[ZoneId, J] {
      final def apply(a: ZoneId): J = J.fromString(a.getId)
    }

  /**
   * @group Time
   */
  final def encodeLocalDateWithFormatter[J: JsonFactory](formatter: DateTimeFormatter): Encoder[LocalDate, J] =
    new JavaTimeEncoder[LocalDate, J] {
      protected[this] final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  final def encodeLocalTimeWithFormatter[J: JsonFactory](formatter: DateTimeFormatter): Encoder[LocalTime, J] =
    new JavaTimeEncoder[LocalTime, J] {
      protected[this] final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  final def encodeLocalDateTimeWithFormatter[J: JsonFactory](formatter: DateTimeFormatter): Encoder[LocalDateTime, J] =
    new JavaTimeEncoder[LocalDateTime, J] {
      protected[this] final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  final def encodeMonthDayWithFormatter[J: JsonFactory](formatter: DateTimeFormatter): Encoder[MonthDay, J] =
    new JavaTimeEncoder[MonthDay, J] {
      protected[this] final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  final def encodeOffsetTimeWithFormatter[J: JsonFactory](formatter: DateTimeFormatter): Encoder[OffsetTime, J] =
    new JavaTimeEncoder[OffsetTime, J] {
      protected[this] final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  final def encodeOffsetDateTimeWithFormatter[J: JsonFactory](formatter: DateTimeFormatter): Encoder[OffsetDateTime, J] =
    new JavaTimeEncoder[OffsetDateTime, J] {
      protected[this] final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  final def encodeYearWithFormatter[J: JsonFactory](formatter: DateTimeFormatter): Encoder[Year, J] =
    new JavaTimeEncoder[Year, J] {
      protected[this] final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  final def encodeYearMonthWithFormatter[J: JsonFactory](formatter: DateTimeFormatter): Encoder[YearMonth, J] =
    new JavaTimeEncoder[YearMonth, J] {
      protected[this] final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  final def encodeZonedDateTimeWithFormatter[J: JsonFactory](formatter: DateTimeFormatter): Encoder[ZonedDateTime, J] =
    new JavaTimeEncoder[ZonedDateTime, J] {
      protected[this] final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
  final def encodeZoneOffsetWithFormatter[J: JsonFactory](formatter: DateTimeFormatter): Encoder[ZoneOffset, J] =
    new JavaTimeEncoder[ZoneOffset, J] {
      protected[this] final def format: DateTimeFormatter = formatter
    }

  /**
   * @group Time
   */
    implicit final def encodeLocalDate[J](implicit J: JsonFactory[J]): Encoder[LocalDate, J] =
      new Encoder[LocalDate, J] {
        final def apply(a: LocalDate): J = J.fromString(a.toString)
      }

  /**
   * @group Time
   */
  implicit final def encodeLocalTime[J: JsonFactory]: Encoder[LocalTime, J] =
    new JavaTimeEncoder[LocalTime, J] {
      protected[this] final def format: DateTimeFormatter = ISO_LOCAL_TIME
    }

  /**
   * @group Time
   */
  implicit final def encodeLocalDateTime[J]: Encoder[LocalDateTime, J] =
    new JavaTimeEncoder[LocalDateTime, J] {
      protected[this] final def format: DateTimeFormatter = ISO_LOCAL_DATE_TIME
    }

  /**
   * @group Time
   */
  implicit final def encodeMonthDay[J]: Encoder[MonthDay, J] = new Encoder[MonthDay, J] {
    final def apply(a: MonthDay): J = J.fromString(a.toString)
  }

  /**
   * @group Time
   */
  implicit final def encodeOffsetTime[J]: Encoder[OffsetTime, J] =
    new JavaTimeEncoder[OffsetTime, J] {
      protected final def format: DateTimeFormatter = ISO_OFFSET_TIME
    }

  /**
   * @group Time
   */
  implicit final def encodeOffsetDateTime[J]: Encoder[OffsetDateTime, J] =
    new JavaTimeEncoder[OffsetDateTime, J] {
      protected final def format: DateTimeFormatter = ISO_OFFSET_DATE_TIME
    }

  /**
   * @group Time
   */
  implicit final def encodeYear[J: JsonFactory]: Encoder[Year, J] = new Encoder[Year, J] {
    final def apply(a: Year): J = JsonFactory[J].fromString(a.toString)
  }

  /**
   * @group Time
   */
  implicit final def encodeYearMonth[J: JsonFactory]: Encoder[YearMonth, J] = new Encoder[YearMonth, J] {
    final def apply(a: YearMonth): J = JsonFactory[J].fromString(a.toString)
  }

  /**
   * @group Time
   */
  implicit final def encodeZonedDateTime[J: JsonFactory]: Encoder[ZonedDateTime, J] =
    new JavaTimeEncoder[ZonedDateTime, J] {
      protected final def format: DateTimeFormatter = ISO_ZONED_DATE_TIME
    }

  /**
   * @group Time
   */
  implicit final def encodeZoneOffset[J: JsonFactory]: Encoder[ZoneOffset, J] = new Encoder[ZoneOffset, J] {
    final def apply(a: ZoneOffset): J = JsonFactory[J].fromString(a.toString)
  }

  /**
   * A subtype of `Encoder` that statically verifies that the instance encodes
   * either a JSON array or an object.
   *
   * @author Travis Brown
   */
  trait AsRoot[A, J] extends Encoder[A, J]

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
    final def apply[A, J](implicit instance: AsRoot[A, J]): AsRoot[A, J] = instance
  }

  private[circe] class LowPriorityAsRootEncoders {

    /**
     * @group Prioritization
     */
    implicit final def importedAsRootEncoder[A, J](implicit exported: Exported[AsRoot[A, J]]): AsRoot[A, J] =
      exported.instance
  }

  /**
   * A type class that provides a conversion from a value of type `A` to a JSON
   * array.
   *
   * @author Travis Brown
   */
  trait AsArray[A, J] extends AsRoot[A, J] { self =>
    final def apply(a: A): J = J.fromValues(encodeArray(a))

    /**
     * Convert a value to a JSON array.
     */
    def encodeArray(a: A): Vector[J]

    /**
     * Create a new [[AsArray]] by applying a function to a value of type `B` before encoding as
     * an `A`.
     */
    final def contramapArray[B](f: B => A): AsArray[B, J] = new AsArray[B, J] {
      final def encodeArray(a: B): Vector[J] = self.encodeArray(f(a))
    }

    /**
     * Create a new [[AsArray]] by applying a function to the output of this
     * one.
     */
    final def mapJsonArray[K](f: Vector[J] => Vector[K]): AsArray[A, K] = new AsArray[A, K] {
      final def encodeArray(a: A): Vector[K] = f(self.encodeArray(a))
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
    final def apply[A, J](implicit instance: AsArray[A, J]): AsArray[A, J] = instance

    /**
     * Construct an instance from a function.
     *
     * @group Utilities
     */
    final def instance[A, J](f: A => Vector[J]): AsArray[A, J] = new AsArray[A, J] {
      final def encodeArray(a: A): Vector[J] = f(a)
    }

    /**
     * @group Instances
     */
    implicit final def arrayEncoderContravariant[J]: Contravariant[AsArray[?, J]] = new Contravariant[AsArray[?, J]] {
      final def contramap[A, B](e: AsArray[A, J])(f: B => A): AsArray[B, J] = e.contramapArray(f)
    }
  }

  private[circe] class LowPriorityAsArrayEncoders {

    /**
     * @group Prioritization
     */
    implicit final def importedAsArrayEncoder[A, J](implicit exported: Exported[AsArray[A]]): AsArray[A] =
      exported.instance
  }

  /**
   * A type class that provides a conversion from a value of type `A` to a
   * [[JsonObject]].
   *
   * @author Travis Brown
   */
  trait AsObject[A, J] extends AsRoot[A, J] { self =>
    final def apply(a: A): J = Json.fromJsonObject(encodeObject(a))

    /**
     * Convert a value to a J object.
     */
    def encodeObject(a: A): JsonObject[J]

    /**
     * Create a new [[AsObject]] by applying a function to a value of type `B` before encoding as an
     * `A`.
     */
    final def contramapObject[B](f: B => A): AsObject[B] = new AsObject[B] {
      final def encodeObject(a: B): JsonObject[J] = self.encodeObject(f(a))
    }

    /**
     * Create a new [[AsObject]] by applying a function to the output of this
     * one.
     */
    final def mapJsonObject[K](f: JsonObject[J] => JsonObject[K]): AsObject[A, K] =
      new AsObject[A, K] {
        final def encodeObject(a: A): JsonObject[J] = f(self.encodeObject(a))
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
    final def apply[A, J](implicit instance: AsObject[A, J]): AsObject[A, J] = instance

    /**
     * Construct an instance from a function.
     *
     * @group Utilities
     */
    final def instance[A, J](f: A => JsonObject[J]): AsObject[A, J] = new AsObject[A, J] {
      final def encodeObject(a: A): JsonObject[J] = f(a)
    }

    /**
     * @group Instances
     */
    implicit final def objectEncoderContravariant[J]: Contravariant[AsObject[?, J]] = new Contravariant[AsObject[?, J]] {
      final def contramap[A, B](e: AsObject[A, J])(f: B => A): AsObject[B, J] = e.contramapObject(f)
    }
  }

  private[circe] class LowPriorityAsObjectEncoders {

    /**
     * @group Prioritization
     */
    implicit final def importedAsObjectEncoder[A, J](implicit
      exported: Exported[AsObject[A]]
    ): AsObject[A] = exported.instance
  }
}

private[circe] trait MidPriorityEncoders extends LowPriorityEncoders {

  /**
   * @group Collection
   */
  implicit final def encodeIterable[A, C[_]](implicit
    encodeA: Encoder[A, J],
    ev: C[A] => Iterable[A]
  ): Encoder.AsArray[C[A]] = new IterableAsArrayEncoder[A, C](encodeA) {
    final protected def toIterator(a: C[A]): Iterator[A] = ev(a).iterator
  }

  protected[this] abstract class IterableAsArrayEncoder[A, C[_], J](encodeA: Encoder[A, J])
      extends Encoder.AsArray[C[A], J] {
    protected def toIterator(a: C[A]): Iterator[A]

    final def encodeArray(a: C[A]): Vector[J] = {
      val builder = Vector.newBuilder[J]
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
  implicit final def importedEncoder[A, J](implicit exported: Exported[Encoder[A, J]]): Encoder[A, J] = exported.instance
}
