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

package io.circe.pointer

import cats.kernel.Hash
import io.circe.ACursor
import io.circe.Encoder
import io.circe.Json

/**
 * Represents a JSON Pointer that may be either absolute or relative.
 */
sealed abstract class Pointer extends (ACursor => ACursor) {

  /**
   * Attempt to get the value at the location pointed to, returning the history if it doesn't exist.
   */
  final def get(input: Json): Either[PointerFailure, Json] = {
    val navigated = apply(input.hcursor)

    navigated.focus match {
      case Some(value) => Right(value)
      case None        => Left(PointerFailure(navigated.history))
    }
  }

  /**
   * Attempt to get the value at the location pointed to.
   *
   * @note For absolute pointers this method may be faster than [[get]] or [[apply]], since it does not have to track
   * error locations.
   */
  def getOption(input: Json): Option[Json]

  /**
   * Return this pointer as a [[Pointer.Absolute]] if possible.
   */
  def asAbsolute: Option[Pointer.Absolute]

  /**
   * Return this pointer as a [[Pointer.Relative]] if possible.
   */
  def asRelative: Option[Pointer.Relative]
}

object Pointer {

  /**
   * Parse a string representing a JSON Pointer that may be either absolute or relative.
   */
  def parse(input: String): Either[PointerSyntaxError, Pointer] =
    if (input.isEmpty) {
      Right(Empty)
    } else if (isAsciiDigit(input.charAt(0))) {
      parseRelative(input)
    } else if (input.charAt(0) == '/') {
      parseAbsoluteUnsafe(input)
    } else {
      notDigitOrRootError
    }

  /**
   * Parse a string representing an absolute JSON Pointer.
   */
  def parseAbsolute(input: String): Either[PointerSyntaxError, Absolute] =
    if (input.isEmpty) {
      Right(Empty)
    } else if (input.charAt(0) == '/') {
      parseAbsoluteUnsafe(input)
    } else {
      notRootError
    }

  /**
   * Parse a string representing a relative JSON Pointer.
   */
  def parseRelative(input: String): Either[PointerSyntaxError, Relative] = {
    val digits = digitPrefixLength(input)

    if (digits == -1) {
      // There was a leading zero.
      leadingZeroError
    } else if (digits == 0) {
      notDigitError
    } else if (digits > maxIntegerDigits) {
      tooManyDigitsError
    } else {
      val distance = Integer.parseInt(input.substring(0, digits))

      if (input.length == digits) {
        Right(new RelativePointer(distance, Empty))
      } else {
        val c = input.charAt(digits)

        if (c == '/') {
          parseAbsoluteUnsafe(input.substring(digits)).map(new RelativePointer(distance, _))
        } else if (c == '#') {
          if (input.length == digits + 1) {
            Right(new LocationLookupPointer(distance))
          } else {
            Left(PointerSyntaxError(digits + 1, "end of input"))
          }
        } else {
          Left(PointerSyntaxError(digits, "JSON Pointer or #"))
        }
      }
    }
  }

  /**
   * Represents a relative JSON Pointer.
   */
  sealed abstract class Relative extends Pointer {
    def evaluate(c: ACursor): Either[PointerFailure, Relative.Result]
    def distance: Int
    def remainder: Option[Absolute]

    final def asAbsolute: Option[Pointer.Absolute] = None
    final def asRelative: Option[Pointer.Relative] = Some(this)

    protected[this] def navigateUp(c: ACursor, distance: Int): ACursor = {
      var current: ACursor = c
      var i = 0

      while (i < distance) {
        current = current.up
        i += 1
      }

      current
    }
  }

  object Relative {

    /**
     * Represents the result of evaluating a relative JSON Pointer, which may be either a value or a location.
     */
    sealed abstract class Result

    object Result {
      case class Json(value: io.circe.Json) extends Result
      case class Key(value: String) extends Result
      case class Index(value: Int) extends Result

      implicit val hashResult: Hash[Result] = Hash.fromUniversalHashCode
      implicit val encodeResult: Encoder[Result] = new Encoder[Result] {
        def apply(result: Result): io.circe.Json = result match {
          case Json(value)  => value
          case Key(value)   => io.circe.Json.fromString(value)
          case Index(value) => io.circe.Json.fromInt(value)
        }
      }
    }
  }

  implicit val hashPointer: Hash[Pointer] = Hash.fromUniversalHashCode

  private[this] val Empty = new Absolute(Array.empty, Array.empty)

  /**
   * Represents an absolute JSON Pointer.
   */
  final class Absolute private[Pointer] (private val tokenArray: Array[String], asIndexArray: Array[Int])
      extends Pointer {
    def asAbsolute: Option[Pointer.Absolute] = Some(this)
    def asRelative: Option[Pointer.Relative] = None

    def apply(c: ACursor): ACursor = navigate(c, true)

    private[Pointer] def navigate(c: ACursor, resetToRoot: Boolean): ACursor = {
      var current: ACursor = if (resetToRoot) c.root else c
      var i = 0

      while (i < tokenArray.length) {
        val key = tokenArray(i)
        val asIndex = asIndexArray(i)

        if (asIndex != -1) {
          current.values match {
            case Some(values) =>
              if (key == "-") {
                current = current.downN(values.size)
              } else {
                current = current.downN(asIndex)
              }
            case None =>
              current = current.downField(key)
          }
        } else {
          current = current.downField(key)
        }

        i += 1
      }

      current
    }

    def getOption(input: Json): Option[Json] = {
      var current: Json = input
      var i = 0

      while (i < tokenArray.length && current.ne(null)) {
        val key = tokenArray(i)
        val asIndex = asIndexArray(i)

        current = if (asIndex > -1) {
          current.asArray match {
            case Some(values) =>
              if (key == "-" || values.size <= asIndex) {
                null
              } else {
                values(asIndex)
              }
            case None => current.asObject.flatMap(_(key)).orNull
          }
        } else {
          current.asObject.flatMap(_(key)).orNull
        }

        i += 1
      }

      if (current.ne(null)) Some(current) else None
    }

    def tokens: Vector[String] = new scala.collection.mutable.WrappedArray.ofRef(tokenArray).toVector

    override def toString(): String = if (tokenArray.length == 0) {
      ""
    } else {
      tokens.map(_.replaceAll("~", "~0").replaceAll("/", "~1")).mkString("/", "/", "")
    }
    override def hashCode(): Int = java.util.Arrays.hashCode(tokenArray.asInstanceOf[Array[Object]])
    override def equals(that: Any): Boolean = that.isInstanceOf[Absolute] && {
      java.util.Arrays.equals(
        tokenArray.asInstanceOf[Array[Object]],
        that.asInstanceOf[Absolute].tokenArray.asInstanceOf[Array[Object]]
      )
    }
  }

  private[this] final class LocationLookupPointer(val distance: Int) extends Relative {
    def apply(c: ACursor): ACursor = navigateUp(c, distance)
    def remainder: Option[Absolute] = None

    def evaluate(c: ACursor): Either[PointerFailure, Relative.Result] = {
      val navigated = apply(c)

      navigated.index match {
        case Some(index) => Right(Relative.Result.Index(index))
        case None        =>
          navigated.key match {
            case Some(key) => Right(Relative.Result.Key(key))
            case None      => Left(PointerFailure(navigated.history))
          }
      }
    }

    def getOption(input: Json): Option[Json] = if (distance == 0) {
      Some(input)
    } else {
      apply(input.hcursor).focus
    }

    override def toString(): String = s"$distance#"
    override def hashCode(): Int = distance
    override def equals(that: Any): Boolean =
      that.isInstanceOf[LocationLookupPointer] && that.asInstanceOf[LocationLookupPointer].distance == distance
  }

  private[this] final class RelativePointer(val distance: Int, protected val pointer: Absolute) extends Relative {
    def apply(c: ACursor): ACursor = pointer.navigate(navigateUp(c, distance), false)
    def remainder: Option[Absolute] = Some(pointer)

    def evaluate(c: ACursor): Either[PointerFailure, Relative.Result] = {
      val navigated = apply(c)

      navigated.focus match {
        case Some(value) => Right(Relative.Result.Json(value))
        case None        => Left(PointerFailure(navigated.history))
      }
    }

    def getOption(input: Json): Option[Json] = if (distance == 0) {
      pointer.getOption(input)
    } else {
      apply(input.hcursor).focus
    }

    override def toString(): String = s"$distance$pointer"
    override def hashCode(): Int = distance + pointer.hashCode
    override def equals(that: Any): Boolean =
      that.isInstanceOf[RelativePointer] && {
        val other = that.asInstanceOf[RelativePointer]

        other.distance == distance && other.pointer == pointer
      }
  }

  private[this] val maxIntegerDigits: Int = 9

  private[this] val notRootError: Either[PointerSyntaxError, Absolute] =
    Left(PointerSyntaxError(0, "/"))
  private[this] val notDigitError: Either[PointerSyntaxError, Relative] =
    Left(PointerSyntaxError(0, "digit"))
  private[this] val notDigitOrRootError: Either[PointerSyntaxError, Pointer] =
    Left(PointerSyntaxError(0, "/ or digit"))
  private[this] val leadingZeroError: Either[PointerSyntaxError, Relative] =
    Left(PointerSyntaxError(1, "JSON Pointer or #"))
  private[this] val tooManyDigitsError: Either[PointerSyntaxError, Relative] =
    Left(PointerSyntaxError(maxIntegerDigits, "JSON Pointer or #"))

  private[this] def isAsciiDigit(c: Char): Boolean = c >= '0' && c <= '9'

  // Returns -1 to indicate a leading zero.
  private[this] def digitPrefixLength(input: String): Int = {
    val length = input.length
    var i = 0

    if (length > 0) {
      if (input.charAt(i) == '0') {
        if (length == 1 || !isAsciiDigit(input.charAt(i + 1))) {
          1
        } else {
          -1
        }
      } else {
        while ((i < length) && isAsciiDigit(input.charAt(i))) {
          i += 1
        }

        i
      }
    } else {
      0
    }
  }

  // Assumes that the input starts with '/'.
  private[this] def parseAbsoluteUnsafe(input: String): Either[PointerSyntaxError, Absolute] = {
    var parts = input.split("/", -1)
    if (parts.length == 0) {
      parts = Array("/", "")
    }
    var pos = 1
    var i = 1
    val tokenArray = new Array[String](parts.length - 1)
    val asIndexArray = new Array[Int](parts.length - 1)

    while (i < parts.length) {
      val part = parts(i)
      val digits = digitPrefixLength(parts(i))

      if (digits == part.length && digits <= maxIntegerDigits && !part.isEmpty) {
        tokenArray(i - 1) = part
        asIndexArray(i - 1) = Integer.parseInt(part)
      } else {
        var previousEscapeEnd = 0
        var currentTildeIndex = part.indexOf('~')

        if (currentTildeIndex == -1) {
          tokenArray(i - 1) = part
          asIndexArray(i - 1) = -1
        } else {
          val builder = new StringBuilder()

          while (currentTildeIndex != -1) {
            if (part.length > currentTildeIndex + 1) {
              builder.append(part.substring(previousEscapeEnd, currentTildeIndex))
              val next = part.charAt(currentTildeIndex + 1)

              if (next == '0') {
                builder.append('~')
              } else if (next == '1') {
                builder.append('/')
              } else {
                return Left(PointerSyntaxError(pos + currentTildeIndex + 1, "0 or 1"))
              }
            } else {
              return Left(PointerSyntaxError(pos + currentTildeIndex, "token character"))
            }

            previousEscapeEnd = currentTildeIndex + 2
            currentTildeIndex = part.indexOf('~', previousEscapeEnd)
          }

          builder.append(part.substring(previousEscapeEnd))

          tokenArray(i - 1) = builder.toString
          asIndexArray(i - 1) = -1
        }
      }

      pos += part.length + 1
      i += 1
    }

    Right(new Absolute(tokenArray, asIndexArray))
  }
}
