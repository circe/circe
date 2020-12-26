package io.circe.pointer

import io.circe.{ ACursor, Json }

sealed abstract class Pointer extends (ACursor => ACursor) {
  final def get(input: Json): Either[PointerFailure, Json] = {
    val navigated = apply(input.hcursor)

    navigated.focus match {
      case Some(value) => Right(value)
      case None        => Left(PointerFailure(navigated.history))
    }
  }
}

object Pointer {
  def parse(input: String, supportRelative: Boolean = true): Either[PointerSyntaxError, Pointer] = {
    if (input.length == 0) {
      Right(empty)
    } else {
      if (supportRelative && isAsciiDigit(input.charAt(0))) {
        Relative.parse(input)
      } else if (input.charAt(0) == '/') {
        var parts = input.split("/")
        if (parts.length == 0) {
          parts = Array("/", "")
        }
        var pos = 1
        var i = 1
        val tokens = Vector.newBuilder[(String, Int)]

        while (i < parts.length) {
          val part = parts(i)
          if (part == "-") {
            tokens += hyphenToken
          } else {
            val digits = digitPrefixLength(parts(i))

            if (digits == part.length && !part.isEmpty) {
              tokens += ((part, Integer.parseInt(part)))
            } else {
              var previousEscapeEnd = 0
              var currentTildeIndex = part.indexOf('~')

              if (currentTildeIndex == -1) {
                tokens += ((part, -1))
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

                tokens += ((builder.toString, -1))
              }
            }
          }

          pos += part.length + 1
          i += 1
        }

        Right(TokensPointer(tokens.result()))
      } else {
        val message = if (supportRelative) "/ or digit" else "/"

        Left(PointerSyntaxError(0, message))
      }
    }
  }

  private[this] val hyphenToken: (String, Int) = ("-", Int.MaxValue)

  val empty: Pointer = Empty

  private[this] case object Empty extends Pointer {
    def apply(c: ACursor): ACursor = c
    override def toString(): String = ""
  }

  private[this] case class TokensPointer(tokens: Vector[(String, Int)]) extends Pointer {
    def apply(c: ACursor): ACursor = {
      var current = c

      tokens.foreach {
        case (key, asIndex) =>
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
      }

      current
    }
    override def toString(): String =
      tokens.map(_._1.replaceAll("~", "~0").replaceAll("/", "~1")).mkString("/", "/", "")
  }

  sealed abstract class Relative extends Pointer {
    def evaluate(c: ACursor): Either[PointerFailure, Relative.Result]
    def distance: Int
    def path: Option[Pointer]

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
    sealed abstract class Result

    object Result {
      case class Json(value: io.circe.Json) extends Result
      case class Key(value: String) extends Result
      case class Index(value: Int) extends Result
    }

    def parse(input: String): Either[PointerSyntaxError, Relative] = {
      val digits = digitPrefixLength(input)

      if (digits == -1) {
        // There was a leading zero.
        Left(PointerSyntaxError(1, "JSON Pointer or #"))
      } else if (digits == 0) {
        Left(PointerSyntaxError(0, "digit"))
      } else if (digits > 9) {
        Left(PointerSyntaxError(9, "JSON Pointer or #"))
      } else {
        val distance = java.lang.Integer.parseInt(input.substring(0, digits))

        if (input.length == digits) {
          Right(PointerRelative(distance, Pointer.empty))
        } else {
          val c = input.charAt(digits)

          if (c == '/') {
            Pointer.parse(input.substring(digits), false).map(PointerRelative(distance, _))
          } else if (c == '#') {
            if (input.length == digits + 1) {
              Right(LocationLookupRelative(distance))
            } else {
              Left(PointerSyntaxError(digits + 1, "end of input"))
            }
          } else {
            Left(PointerSyntaxError(digits, "JSON Pointer or #"))
          }
        }
      }
    }

    private[this] case class LocationLookupRelative(distance: Int) extends Relative {
      final def apply(c: ACursor): ACursor = navigateUp(c, distance)
      final def path: Option[Pointer] = None

      final def evaluate(c: ACursor): Either[PointerFailure, Result] = {
        val navigated = apply(c)

        navigated.index match {
          case Some(index) => Right(Result.Index(index))
          case None =>
            navigated.key match {
              case Some(key) => Right(Result.Key(key))
              case None      => Left(PointerFailure(navigated.history))
            }
        }
      }
      final override def toString(): String = s"$distance#"
    }

    private[this] case class PointerRelative(distance: Int, pointer: Pointer) extends Relative {
      final def apply(c: ACursor): ACursor = pointer(navigateUp(c, distance))
      final def path: Option[Pointer] = Some(pointer)

      final def evaluate(c: ACursor): Either[PointerFailure, Result] = {
        val navigated = apply(c)

        navigated.focus match {
          case Some(value) => Right(Result.Json(value))
          case None        => Left(PointerFailure(navigated.history))
        }
      }
      final override def toString(): String = s"$distance$pointer"
    }
  }

  private[this] def isAsciiDigit(c: Char): Boolean = c >= '0' && c <= '9'

  // Returns -1 to indicate a leading zero.
  private[this] def digitPrefixLength(input: String): Int = {
    var i = 0
    val length = input.length

    if (length > 0) {
      if (input.charAt(i) == '0') {
        if (length == 1 || !isAsciiDigit(input.charAt(i + 1))) {
          1
        } else {
          -1
        }
      } else {
        while ((i < input.length) && isAsciiDigit(input.charAt(i))) {
          i += 1
        }

        i
      }
    } else {
      0
    }
  }
}
