package io.circe.flat

import java.util.{ ArrayList, List => JList }
import scala.annotation.switch

final class Overlay private (
  private[this] val input: String,
  val blockSize: Int
) {
  private[this] val length = input.length
  private[this] val blocks: JList[Array[Short]] = new ArrayList[Array[Short]](2)
  private[this] var current: Int = 0
  private[this] var failure: ParseFailure = null

  private[this] final val VALUE = 0
  private[this] final val OBJECT_START = 1
  private[this] final val OBJECT_COMMA = 2
  private[this] final val OBJECT_KEY = 3
  private[this] final val ARRAY_START = 4
  private[this] final val ARRAY_COMMA = 5
  private[this] final val DONE = 6

  private[this] final val initialStackSize = 4

  this.parse()

  def getBlocks: Either[ParseFailure, JList[Array[Short]]] =
    if (failure.eq(null)) {
      //import scala.collection.JavaConverters._

      Right(blocks) //blocks.asScala.map(Vector(_: _*)).toList)
    } else {
      Left(failure)
    }

  def getBlocksUnsafe: JList[Array[Short]] = blocks
  def getFailure: ParseFailure = failure

  private[this] def startElement(jsonType: Int, start: Int): Int = createElement(jsonType, start, -1, -1)
  private[this] def endElement(element: Int, end: Int, nested: Int): Int = {
    val block = this.getBlock(element)
    val index = this.getBlockIndex(element)

    block(index + Overlay.EndOffset) = end.toShort
    block(index + Overlay.NestedOffset) = nested.toShort

    end + 1
  }

  private[this] def createElement(jsonType: Int, start: Int, end: Int, nested: Int): Int = {
    val currentBlock: Int = (this.current * 4) / this.blockSize

    if (currentBlock == this.blocks.size) {
      this.blocks.add(new Array[Short](this.blockSize))
    }

    val block = this.blocks.get(currentBlock)
    val index = this.getBlockIndex(this.current)

    block(index) = jsonType.toShort
    block(index + Overlay.StartOffset) = start.toShort
    block(index + Overlay.EndOffset) = end.toShort
    block(index + Overlay.NestedOffset) = nested.toShort

    this.current += 1

    end + 1
  }

  @inline private[this] def getBlock(element: Int): Array[Short] = this.blocks.get((element * 4) / this.blockSize)
  @inline private[this] def getBlockIndex(element: Int): Int = (element * 4) % this.blockSize

  def parse(): Unit = {
    var i = 0
    var depth = -1
    var stack: Array[Byte] = null
    var state: Int = VALUE

    while (state != DONE) {
      i = this.skipWhitespace(i)
      val c = this.input.charAt(i)

      (state: @switch) match {
        case VALUE =>
          if (c == '{') {
            if (stack.eq(null)) {
              stack = new Array[Byte](initialStackSize)
            } else if (stack.length <= depth + 1) {
              val newStack = new Array[Byte](stack.length * 2)
              System.arraycopy(stack, 0, newStack, 0, stack.length)
              stack = newStack
            }
            depth += 1
            stack(depth) = (this.current + 1).toByte
            state = OBJECT_START
            this.startElement(Overlay.ObjectType, i)
            i += 1
          } else if (c == '[') {
            if (stack.eq(null)) {
              stack = new Array[Byte](initialStackSize)
            } else if (stack.length <= depth + 1) {
              val newStack = new Array[Byte](stack.length * 2)
              System.arraycopy(stack, 0, newStack, 0, stack.length)
              stack = newStack
            }
            depth += 1
            stack(depth) = (-(this.current + 1)).toByte
            state = ARRAY_START
            this.startElement(Overlay.ArrayType, i)
            i += 1
          } else {
            (c: @switch) match {
              case '"' =>
                i = parseString(i)
              case '0' | '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' | '-' =>
                if (depth == -1) {
                  i = parseNumberTop(i)
                } else {
                  i = parseNumber(i)
                }
              case 't' =>
                i = parseTrue(i)
              case 'f' =>
                i = parseFalse(i)
              case 'n' =>
                i = parseNull(i)
              case other =>
                this.failure = ExpectedValueCharFailure(i, other)
                return
            }

            if (depth == -1) {
              state = DONE
            } else if (stack(depth) > 0) {
              state = OBJECT_COMMA
            } else {
              state = ARRAY_COMMA
            }
          }
        case OBJECT_START =>
          if (c == '}') {
            val element = stack(depth) - 1
            depth -= 1
            this.endElement(element, i, this.current - element - 1)
            i += 1

            if (depth == -1) {
              state = DONE
            } else if (stack(depth) > 0) {
              state = OBJECT_COMMA
            } else {
              state = ARRAY_COMMA
            }
          } else {
            state = OBJECT_KEY
          }
        case OBJECT_COMMA =>
          if (c == ',') {
            state = OBJECT_KEY
            i += 1
          } else if (c == '}') {
            val element = stack(depth) - 1
            depth -= 1
            this.endElement(element, i, this.current - element - 1)
            i += 1

            if (depth == -1) {
              state = DONE
            } else if (stack(depth) > 0) {
              state = OBJECT_COMMA
            } else {
              state = ARRAY_COMMA
            }
          } else {
            this.failure = Overlay.failObjectComma(i, c)
            return
          }
        case OBJECT_KEY =>
          i = this.parseString(i)

          if (i == -1) {
            return
          }

          i = this.skipWhitespace(i)

          if (!this.expectChar(i, ':')) {
            return
          }

          i += 1
          state = VALUE
        case ARRAY_START =>
          if (c == ']') {
            val element = -stack(depth) - 1
            depth -= 1
            this.endElement(element, i, this.current - element - 1)
            i += 1

            if (depth == -1) {
              state = DONE
            } else if (stack(depth) > 0) {
              state = OBJECT_COMMA
            } else {
              state = ARRAY_COMMA
            }
          } else {
            state = VALUE
          }
        case ARRAY_COMMA =>
          if (c == ',') {
            state = VALUE
            i += 1
          } else if (c == ']') {
            val element = -stack(depth) - 1
            depth -= 1
            this.endElement(element, i, this.current - element - 1)
            i += 1

            if (depth == -1) {
              state = DONE
            } else if (stack(depth) > 0) {
              state = OBJECT_COMMA
            } else {
              state = ARRAY_COMMA
            }
          } else {
            this.failure = Overlay.failArrayComma(i, c)
            return
          }
        case _ =>
          state = DONE
      }
    }

    if (i != -1) {
      val last = this.skipWhitespaceTop(i)
      if (last != this.length) {
        this.failure = ExpectedEosFailure(last, this.input.charAt(last))
      }
    }
  }

  private[this] def parseNumber(start: Int): Int = {
    var i = start
    var seenMinus = false
    var seenLeadingZero = false
    var seenDot = false
    var seenExponent = false

    while (true) {
      val c = this.input.charAt(i)

      (c: @switch) match {
        case '-' =>
          if (i > start) {
            this.failure = UnexpectedNumberCharFailure(i, c)
            return -1
          }
          seenMinus = true
          i += 1
        case 'e' | 'E' =>
          if (seenExponent) {
            this.failure = UnexpectedNumberCharFailure(i, c)
            return -1
          }

          seenLeadingZero = false
          seenExponent = true

          i += 1
          val next = this.input.charAt(i)

          if (next == '-' || next == '+') {
            i += 1
            val digit = this.input.charAt(i)
            if (digit < '0' || digit > '9') {
              this.failure = UnexpectedNumberCharFailure(i, digit)
              return -1
            }
          } else if (next < '0' || next > '9') {
            this.failure = UnexpectedNumberCharFailure(i, next)
            return -1
          }
          i += 1
        case '.' =>
          if (seenDot) {
            this.failure = UnexpectedNumberCharFailure(i, c)
            return -1
          }

          if (i == start || (seenMinus && (i == start + 1))) {
            this.failure = UnexpectedNumberCharFailure(i, c)
            return -1
          }

          seenLeadingZero = false
          seenDot = true
          i += 1
        case '0' =>
          if (i == start) {
            seenLeadingZero = true
          }
          i += 1
        case '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' =>
          if (seenLeadingZero) {
            this.failure = UnexpectedNumberCharFailure(i, c)
            return -1
          }
          i += 1
        case _ =>
          if (seenMinus && (start == i - 1)) {
            this.failure = UnexpectedNumberCharFailure(start, '-')
            return -1
          }

          return this.createElement(Overlay.NumberType, start, i - 1, 0)
      }
    }

    -1
  }

  private[this] def parseNumberTop(start: Int): Int = {
    var i = start
    var seenMinus = false
    var seenLeadingZero = false
    var seenDot = false
    var seenExponent = false

    while (i < this.length) {
      val c = this.input.charAt(i)

      (c: @switch) match {
        case '-' =>
          if (i > start) {
            this.failure = UnexpectedNumberCharFailure(i, c)
            return -1
          }
          seenMinus = true
          i += 1
        case 'e' | 'E' =>
          if (seenExponent) {
            this.failure = UnexpectedNumberCharFailure(i, c)
            return -1
          }

          seenLeadingZero = false
          seenExponent = true

          i += 1
          val next = this.input.charAt(i)

          if (next == '-' || next == '+') {
            i += 1
            val digit = this.input.charAt(i)
            if (digit < '0' || digit > '9') {
              this.failure = UnexpectedNumberCharFailure(i, digit)
              return -1
            }
          } else if (next < '0' || next > '9') {
            this.failure = UnexpectedNumberCharFailure(i, next)
            return -1
          }
          i += 1
        case '.' =>
          if (seenDot) {
            this.failure = UnexpectedNumberCharFailure(i, c)
            return -1
          }

          if (i == start || (seenMinus && (i == start + 1))) {
            this.failure = UnexpectedNumberCharFailure(i, c)
            return -1
          }

          seenLeadingZero = false
          seenDot = true
          i += 1
        case '0' =>
          if (i == start) {
            seenLeadingZero = true
          }
          i += 1
        case '1' | '2' | '3' | '4' | '5' | '6' | '7' | '8' | '9' =>
          if (seenLeadingZero) {
            this.failure = UnexpectedNumberCharFailure(i, c)
            return -1
          }
          i += 1
        case _ =>
          if (seenMinus && (start == i - 1)) {
            this.failure = UnexpectedNumberCharFailure(start, '-')
            return -1
          }

          return this.createElement(Overlay.NumberType, start, i - 1, 0)
      }
    }

    -1
  }

  private[this] final def parseString(start: Int): Int = {
    var escaped = false
    var i = start + 1

    while (true) {
      val c = this.input.charAt(i)

      if (c == '"') {
        val jsonType = if (escaped) Overlay.StringEscapedType else Overlay.StringType
        return this.createElement(jsonType, start, i, 0)
      } else {
        if (c < 32) {
          this.failure = IllegalControlCharFailure(i, c.toInt)
          return -1
        } else if (c == '\\') {
          escaped = true
          i += 1
          val next = this.input.charAt(i)

          (next: @switch) match {
            case '"' | '/' | '\\' | 'b' | 'f' | 'n' | 'r' | 't' =>
            case 'u' =>
              if (
                !this.expectHex(i + 1) || !this.expectHex(i + 2) || !this.expectHex(i + 3) || !this.expectHex(i + 4)
              ) {
                return -1
              }

              i += 4
            case _ =>
              this.failure = IllegalEscapeCharFailure(i, next)
              return -1
          }
        }

        i += 1
      }
    }

    -1
  }

  private[this] def parseNull(start: Int): Int = {
    val remaining = this.length - start

    if (remaining < 4) {
      this.failure = UnexpectedEosFailure(start + remaining - 1)
      -1
    } else {
      if (expectChar(start + 1, 'u') && expectChar(start + 2, 'l') && expectChar(start + 3, 'l')) {
        createElement(Overlay.NullType, start, start + 3, 0)
      } else {
        -1
      }
    }
  }

  private[this] def parseTrue(start: Int): Int = {
    val remaining = this.length - start

    if (remaining < 4) {
      this.failure = UnexpectedEosFailure(start + remaining - 1)
      -1
    } else {
      if (expectChar(start + 1, 'r') && expectChar(start + 2, 'u') && expectChar(start + 3, 'e')) {
        createElement(Overlay.TrueType, start, start + 3, 0)
      } else {
        -1
      }
    }
  }

  private[this] def parseFalse(start: Int): Int = {
    val remaining = this.length - start

    if (remaining < 5) {
      this.failure = UnexpectedEosFailure(start + remaining - 1)
      -1
    } else {
      if (
        expectChar(start + 1, 'a') && expectChar(start + 2, 'l') && expectChar(start + 3, 's') && expectChar(
          start + 4,
          'e'
        )
      ) {
        createElement(Overlay.FalseType, start, start + 4, 0)
      } else {
        -1
      }
    }
  }

  @inline private[this] final def skipWhitespace(start: Int): Int = {
    var i = start

    while (true) {
      val c = this.input.charAt(i)

      (c: @switch) match {
        case ' ' | '\t' | '\n' | '\r' => i += 1
        case _                        => return i
      }
    }

    i
  }

  private[this] final def skipWhitespaceTop(start: Int): Int = {
    var i = start

    while (i < this.length) {
      val c = this.input.charAt(i)

      (c: @switch) match {
        case ' ' | '\t' | '\n' | '\r' => i += 1
        case _                        => return i
      }
    }

    i
  }

  private[this] final def expectChar(i: Int, expected: Char): Boolean = {
    val c = this.input.charAt(i)

    if (c != expected) {
      this.failure = ExpectedCharFailure(i, c, expected)
      false
    } else {
      true
    }
  }

  private[this] def expectHex(i: Int): Boolean = {
    val c = this.input.charAt(i)

    if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')) {
      this.failure = ExpectedHexCharFailure(i, c)
      false
    } else {
      true
    }
  }

}

final object Overlay {
  final val NullType = 0
  final val TrueType = 1
  final val FalseType = 2
  final val StringType = 3
  final val StringEscapedType = 4
  final val NumberType = 5
  final val ArrayType = 6
  final val ObjectType = 7

  final val TypeOffset = 0
  final val StartOffset = 1
  final val EndOffset = 2
  final val NestedOffset = 3

  private[this] def calculateBlockSize(length: Int): Int = 4 * math.min(math.max(length / 16, 4), 1024)

  def parseUnsafe(input: String): Overlay = new Overlay(input, calculateBlockSize(input.length))

  private final def failObjectComma(pos: Int, value: Char): ParseFailure =
    ExpectedCharsFailure(pos, value, List(',', '}'))
  private final def failArrayComma(pos: Int, value: Char): ParseFailure =
    ExpectedCharsFailure(pos, value, List(',', ']'))
}

sealed abstract class ParseFailure extends Exception {
  def pos: Int
  def baseMessage: String
  override final def getMessage(): String = s"$baseMessage at $pos"
}

case class ExpectedEosFailure(pos: Int, value: Char) extends ParseFailure {
  def baseMessage: String = s"Expected whitespace or end-of-string"
}

case class ExpectedCharFailure(pos: Int, value: Char, expected: Char) extends ParseFailure {
  def baseMessage: String = s"Expected '$expected'"
}

case class ExpectedCharsFailure(pos: Int, value: Char, expected: List[Char]) extends ParseFailure {
  def baseMessage: String = s"Expected one of ${expected.map(c => s"'$c'").mkString(", ")}"
}

case class ExpectedHexCharFailure(pos: Int, value: Char) extends ParseFailure {
  def baseMessage: String = s"Expected a hexadecimal character"
}

case class ExpectedValueCharFailure(pos: Int, value: Char) extends ParseFailure {
  def baseMessage: String = s"Expected a JSON value"
}

case class IllegalControlCharFailure(pos: Int, value: Int) extends ParseFailure {
  def baseMessage: String = s"Illegal control character $value"
}

case class IllegalEscapeCharFailure(pos: Int, value: Char) extends ParseFailure {
  def baseMessage: String = s"Illegal escape character '$value'"
}

case class UnexpectedEosFailure(pos: Int) extends ParseFailure {
  def baseMessage: String = s"Unexpected end-of-string"
}
case class UnexpectedNumberCharFailure(pos: Int, value: Char) extends ParseFailure {
  def baseMessage: String = s"Unexpected number character '$value'"
}
