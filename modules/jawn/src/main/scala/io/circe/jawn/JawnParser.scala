package io.circe.jawn

import cats.data.ValidatedNel
import io.circe.{ Decoder, Error, Json, Parser, ParsingFailure }
import java.io.File
import java.nio.ByteBuffer
import scala.util.{ Failure, Success, Try }

final object JawnParser {

  /**
   * Returns a parser that fails on JSON strings, object keys, or numbers that
   * exceed a given length.
   *
   * In some cases excessively long values (e.g. JSON numbers with millions of
   * digits) may support denial-of-service attacks. For example, the string
   * constructor for Java's `BigInteger` is quadratic with the length of the
   * input, and decoding a ten-million digit JSON number into a `BigInteger` may
   * take minutes.
   */
  def apply(maxValueSize: Int): JawnParser = new JawnParser(Some(maxValueSize), allowDuplicateKeys = true)

  /**
   * Returns a parser that fails on:
   * <ul>
   *   <li>JSON strings, object keys, or numbers that exceed a given length </li>
   *   <li>encountering duplicate keys as per JSONlint</li>
   * </ul>
   *
   * In some cases excessively long values (e.g. JSON numbers with millions of
   * digits) may support denial-of-service attacks. For example, the string
   * constructor for Java's `BigInteger` is quadratic with the length of the
   * input, and decoding a ten-million digit JSON number into a `BigInteger` may
   * take minutes.
   */
  def apply(maxValueSize: Int, allowDuplicateKeys: Boolean): JawnParser =
    new JawnParser(Some(maxValueSize), allowDuplicateKeys)
}

class JawnParser(maxValueSize: Option[Int], allowDuplicateKeys: Boolean) extends Parser {
  def this() = this(None, true)

  private[this] final val supportParser: CirceSupportParser =
    maxValueSize match {
      case Some(_) => new CirceSupportParser(maxValueSize, allowDuplicateKeys)
      case None    => new CirceSupportParser(None, allowDuplicateKeys)
    }

  private[this] final def fromTry(t: Try[Json]): Either[ParsingFailure, Json] = t match {
    case Success(json)  => Right(json)
    case Failure(error) => Left(ParsingFailure(error.getMessage, error))
  }

  final def parse(input: String): Either[ParsingFailure, Json] =
    fromTry(supportParser.parseFromString(input))

  final def parseFile(file: File): Either[ParsingFailure, Json] =
    fromTry(supportParser.parseFromFile(file))

  final def parseByteBuffer(buffer: ByteBuffer): Either[ParsingFailure, Json] =
    fromTry(supportParser.parseFromByteBuffer(buffer))

  final def decodeByteBuffer[A: Decoder](buffer: ByteBuffer): Either[Error, A] =
    finishDecode[A](parseByteBuffer(buffer))

  final def decodeByteBufferAccumulating[A: Decoder](buffer: ByteBuffer): ValidatedNel[Error, A] =
    finishDecodeAccumulating[A](parseByteBuffer(buffer))

  final def decodeFile[A: Decoder](file: File): Either[Error, A] =
    finishDecode[A](parseFile(file))

  final def decodeFileAccumulating[A: Decoder](file: File): ValidatedNel[Error, A] =
    finishDecodeAccumulating[A](parseFile(file))
}
