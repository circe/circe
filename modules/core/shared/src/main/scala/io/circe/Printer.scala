package io.circe

import scala.annotation.{ switch, tailrec }

/**
 * A pretty-printer for JSON values.
 *
 * @author Travis Brown
 * @author Tony Morris
 *
 * @param indent The indentation to use if any format strings contain a new line.
 * @param lbraceLeft Spaces to insert to left of a left brace.
 * @param lbraceRight Spaces to insert to right of a left brace.
 * @param rbraceLeft Spaces to insert to left of a right brace.
 * @param rbraceRight Spaces to insert to right of a right brace.
 * @param lbracketLeft Spaces to insert to left of a left bracket.
 * @param lbracketRight Spaces to insert to right of a left bracket.
 * @param rbracketLeft Spaces to insert to left of a right bracket.
 * @param rbracketRight Spaces to insert to right of a right bracket.
 * @param lrbracketsEmpty Spaces to insert for an empty array.
 * @param arrayCommaLeft Spaces to insert to left of a comma in an array.
 * @param arrayCommaRight Spaces to insert to right of a comma in an array.
 * @param objectCommaLeft Spaces to insert to left of a comma in an object.
 * @param objectCommaRight Spaces to insert to right of a comma in an object.
 * @param colonLeft Spaces to insert to left of a colon.
 * @param colonRight Spaces to insert to right of a colon.
 * @param preserveOrder Determines if field ordering should be preserved.
 * @param dropNullKeys Determines if object fields with values of null are dropped from the output.
 */
final case class Printer(
  preserveOrder: Boolean,
  dropNullKeys: Boolean,
  indent: String,
  lbraceLeft: String = "",
  lbraceRight: String = "",
  rbraceLeft: String = "",
  rbraceRight: String = "",
  lbracketLeft: String = "",
  lbracketRight: String = "",
  rbracketLeft: String = "",
  rbracketRight: String = "",
  lrbracketsEmpty: String = "",
  arrayCommaLeft: String = "",
  arrayCommaRight: String = "",
  objectCommaLeft: String = "",
  objectCommaRight: String = "",
  colonLeft: String = "",
  colonRight: String = ""
) extends Serializable {
  private[this] final val openBraceText = "{"
  private[this] final val closeBraceText = "}"
  private[this] final val openArrayText = "["
  private[this] final val closeArrayText = "]"
  private[this] final val commaText = ","
  private[this] final val colonText = ":"
  private[this] final val nullText = "null"
  private[this] final val trueText = "true"
  private[this] final val falseText = "false"
  private[this] final val stringEnclosureText = "\""

  private[this] final def addIndentation(s: String): Int => String = {
    val lastNewLineIndex = s.lastIndexOf("\n")
    if (lastNewLineIndex < 0) {
      _ => s
    } else {
      val afterLastNewLineIndex = lastNewLineIndex + 1
      val start = s.substring(0, afterLastNewLineIndex)
      val end = s.substring(afterLastNewLineIndex)
      n => start + Predef.augmentString(indent) * n + end
    }
  }

  private[this] final val pieces = new Printer.MemoizedPieces {
    final def compute(i: Int): Printer.Pieces = Printer.Pieces(
      String.format(
        "%s%s%s",
        addIndentation(lbraceLeft)(i),
        openBraceText,
        addIndentation(lbraceRight)(i + 1)
      ),
      String.format(
        "%s%s%s",
        addIndentation(rbraceLeft)(i),
        closeBraceText,
        addIndentation(rbraceRight)(i + 1)
      ),
      String.format(
        "%s%s%s",
        addIndentation(lbracketLeft)(i),
        openArrayText,
        addIndentation(lbracketRight)(i + 1)
      ),
      String.format(
        "%s%s%s",
        addIndentation(rbracketLeft)(i),
        closeArrayText,
        addIndentation(rbracketRight)(i + 1)
      ),
      String.format(
        "%s%s%s",
        openArrayText,
        addIndentation(lrbracketsEmpty)(i),
        closeArrayText
      ),
      String.format(
        "%s%s%s",
        addIndentation(arrayCommaLeft)(i + 1),
        commaText,
        addIndentation(arrayCommaRight)(i + 1)
      ),
      String.format(
        "%s%s%s",
        addIndentation(objectCommaLeft)(i + 1),
        commaText,
        addIndentation(objectCommaRight)(i + 1)
      ),
      String.format(
        "%s%s%s",
        addIndentation(colonLeft)(i + 1),
        colonText,
        addIndentation(colonRight)(i + 1)
      )
    )
  }

  /**
   * Returns a string representation of a pretty-printed JSON value.
   */
  final def pretty(j: Json): String = {
    val builder = new java.lang.StringBuilder()

    @tailrec
    def appendJsonString(
      jsonString: String,
      normalChars: Boolean,
      offset: Int
    ): Unit = if (normalChars) {
      var i = offset

      while (i < jsonString.length && Printer.isNormalChar(jsonString.charAt(i))) {
        i += 1
      }

      builder.append(jsonString, offset, i)

      if (i < jsonString.length) appendJsonString(jsonString, false, i)
    } else {
      var i = offset

      while (i < jsonString.length && !Printer.isNormalChar(jsonString.charAt(i))) {
        builder.append(Printer.escape(jsonString.charAt(i)))
        i += 1
      }

      if (i < jsonString.length) appendJsonString(jsonString, true, i)
    }

    def encloseJsonString(jsonString: String): Unit = {
      builder.append(stringEnclosureText)
      appendJsonString(jsonString, true, 0)
      builder.append(stringEnclosureText)
    }

    def trav(depth: Int, k: Json): Unit = {
      val p = pieces(depth)

      k match {
        case Json.JObject(o) =>
          builder.append(p.lBraces)
          val items = if (preserveOrder) o.toList else o.toMap
          var first = true

          val itemIterator = items.iterator

          while (itemIterator.hasNext) {
            val (key, value) = itemIterator.next()
            if (!dropNullKeys || !value.isNull) {
              if (!first) {
                builder.append(p.objectCommas)
              }
              encloseJsonString(key)
              builder.append(p.colons)
              trav(depth + 1, value)
              first = false
            }
          }
          builder.append(p.rBraces)
        case Json.JString(s) => encloseJsonString(s)
        case Json.JNumber(n) => builder.append(n.toString)
        case Json.JBoolean(b) => builder.append(if (b) trueText else falseText)
        case Json.JArray(arr) =>
          val arrIterator = arr.iterator
          if (!arrIterator.hasNext) builder.append(p.lrEmptyBrackets) else {
            builder.append(p.lBrackets)
            trav(depth + 1, arrIterator.next)

            while (arrIterator.hasNext) {
              builder.append(p.arrayCommas)
              trav(depth + 1, arrIterator.next())
            }
            builder.append(p.rBrackets)
          }
        case Json.JNull => builder.append(nullText)
      }
    }

    trav(0, j)
    builder.toString
  }
}


final object Printer {
  /**
   * A pretty-printer configuration that inserts no spaces.
   */
  final val noSpaces: Printer = Printer(
    preserveOrder = true,
    dropNullKeys = false,
    indent = ""
  )

  /**
   * A pretty-printer configuration that indents by the given spaces.
   */
  final def indented(indent: String): Printer = Printer(
    preserveOrder = true,
    dropNullKeys = false,
    indent = indent,
    lbraceRight = "\n",
    rbraceLeft = "\n",
    lbracketRight = "\n",
    rbracketLeft = "\n",
    lrbracketsEmpty = "\n",
    arrayCommaRight = "\n",
    objectCommaRight = "\n",
    colonLeft = " ",
    colonRight = " "
  )

  /**
   * A pretty-printer configuration that indents by two spaces.
   */
  final val spaces2: Printer = indented("  ")

  /**
   * A pretty-printer configuration that indents by four spaces.
   */
  final val spaces4: Printer = indented("    ")

  private[circe] final def escape(c: Char): String = (c: @switch) match {
    case '\\' => "\\\\"
    case '"' => "\\\""
    case '\b' => "\\b"
    case '\f' => "\\f"
    case '\n' => "\\n"
    case '\r' => "\\r"
    case '\t' => "\\t"
    case possibleUnicode => if (Character.isISOControl(possibleUnicode)) {
      String.format("\\u%04x", Integer.valueOf(possibleUnicode.toInt))
    } else possibleUnicode.toString
  }

  private[circe] final def isNormalChar(c: Char): Boolean = (c: @switch) match {
    case '\\' => false
    case '"' => false
    case '\b' => false
    case '\f' => false
    case '\n' => false
    case '\r' => false
    case '\t' => false
    case possibleUnicode => !Character.isISOControl(possibleUnicode)
  }

  private[circe] final case class Pieces(
    lBraces: String,
    rBraces: String,
    lBrackets: String,
    rBrackets: String,
    lrEmptyBrackets: String,
    arrayCommas: String,
    objectCommas: String,
    colons: String
  ) extends Serializable

  private[this] final val maxMemoizationDepth = 128

  private[circe] abstract class MemoizedPieces extends Serializable {
    def compute(i: Int): Pieces

    private[this] final val known = new java.util.concurrent.CopyOnWriteArrayList[Pieces](
      new Array[Pieces](maxMemoizationDepth)
    )

    final def apply(i: Int): Pieces = if (i >= maxMemoizationDepth) compute(i) else {
      val res = known.get(i)

      if (res != null) res else {
        val tmp = compute(i)
        known.set(i, tmp)
        tmp
      }
    }
  }
}
