package io.circe.jawn

import cats.data.ValidatedNel
import io.circe.{ Decoder, Error, Json, Parser, ParsingFailure }
import java.nio.ByteBuffer
import scala.util.{ Failure, Success, Try }

object JawnParser {

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
   *
   * If `allowDuplicateKeys` is set to `true`, the parser will not fail if it encounters an object
   * containing duplicate keys. Note that duplicate keys are not prohibited by the JSON
   * specification, but many linters and other processors do not handle them.
   */
  def apply(maxValueSize: Int, allowDuplicateKeys: Boolean): JawnParser =
    new JawnParser(Some(maxValueSize), allowDuplicateKeys)

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
  def apply(maxValueSize: Int): JawnParser = JawnParser(maxValueSize, true)

  /**
   * If `allowDuplicateKeys` is set to `true`, the parser will not fail if it encounters an object
   * containing duplicate keys. Note that duplicate keys are not prohibited by the JSON
   * specification, but many linters and other processors do not handle them.
   */
  def apply(allowDuplicateKeys: Boolean): JawnParser = new JawnParser(None, allowDuplicateKeys)
}

class JawnParser(maxValueSize: Option[Int], allowDuplicateKeys: Boolean) extends Parser with JawnParserPlatform {
  def this() = this(None, true)

  private[jawn] final val supportParser: CirceSupportParser =
    maxValueSize match {
      case Some(_) => new CirceSupportParser(maxValueSize, allowDuplicateKeys)
      case None    => new CirceSupportParser(None, allowDuplicateKeys)
    }

  private[jawn] final def fromTry(t: Try[Json]): Either[ParsingFailure, Json] = t match {
    case Success(json)  => Right(json)
    case Failure(error) => Left(ParsingFailure(error.getMessage, error))
  }

  final def parse(str: String): Either[ParsingFailure, Json] =
    fromTry(supportParser.parseFromString(str))

  final def parseCharSequence(cs: CharSequence): Either[ParsingFailure, Json] =
    fromTry(supportParser.parseFromCharSequence(cs))

  final def parseByteBuffer(buffer: ByteBuffer): Either[ParsingFailure, Json] =
    fromTry(supportParser.parseFromByteBuffer(buffer))

  final def decodeCharSequence[A: Decoder](cs: CharSequence): Either[Error, A] =
    finishDecode[A](parseCharSequence(cs))

  final def decodeCharSequenceAccumulating[A: Decoder](cs: CharSequence): ValidatedNel[Error, A] =
    finishDecodeAccumulating[A](parseCharSequence(cs))

  final def parseByteArray(bytes: Array[Byte]): Either[ParsingFailure, Json] =
    fromTry(supportParser.parseFromByteArray(bytes))

  final def decodeByteBuffer[A: Decoder](buffer: ByteBuffer): Either[Error, A] =
    finishDecode[A](parseByteBuffer(buffer))

  final def decodeByteBufferAccumulating[A: Decoder](buffer: ByteBuffer): ValidatedNel[Error, A] =
    finishDecodeAccumulating[A](parseByteBuffer(buffer))

  final def decodeByteArray[A: Decoder](bytes: Array[Byte]): Either[Error, A] =
    finishDecode[A](parseByteArray(bytes))

  final def decodeByteArrayAccumulating[A: Decoder](bytes: Array[Byte]): ValidatedNel[Error, A] =
    finishDecodeAccumulating[A](parseByteArray(bytes))
}
