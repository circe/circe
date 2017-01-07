package io.circe

import java.io.{ BufferedWriter, ByteArrayOutputStream, OutputStreamWriter }
import java.lang.StringBuilder
import scala.annotation.switch

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

  private[this] final def concat(left: String, text: String, right: String): String =
    left.concat(text).concat(right)

  private[this] final val pieces = new Printer.MemoizedPieces {
    final def compute(i: Int): Printer.Pieces = Printer.Pieces(
      concat(
        addIndentation(lbraceLeft)(i),
        openBraceText,
        addIndentation(lbraceRight)(i + 1)
      ),
      concat(
        addIndentation(rbraceLeft)(i),
        closeBraceText,
        addIndentation(rbraceRight)(i + 1)
      ),
      concat(
        addIndentation(lbracketLeft)(i),
        openArrayText,
        addIndentation(lbracketRight)(i + 1)
      ),
      concat(
        addIndentation(rbracketLeft)(i),
        closeArrayText,
        addIndentation(rbracketRight)(i + 1)
      ),
      concat(
        openArrayText,
        addIndentation(lrbracketsEmpty)(i),
        closeArrayText
      ),
      concat(
        addIndentation(arrayCommaLeft)(i + 1),
        commaText,
        addIndentation(arrayCommaRight)(i + 1)
      ),
      concat(
        addIndentation(objectCommaLeft)(i + 1),
        commaText,
        addIndentation(objectCommaRight)(i + 1)
      ),
      concat(
        addIndentation(colonLeft)(i + 1),
        colonText,
        addIndentation(colonRight)(i + 1)
      )
    )
  }

  private[this] final def printEscapedChar(writer: Appendable)(c: Char): Unit = {
    writer.append('\\')
    (c: @switch) match {
      case '\\' => writer.append('\\')
      case '"'  => writer.append('"')
      case '\b' => writer.append('b')
      case '\f' => writer.append('f')
      case '\n' => writer.append('n')
      case '\r' => writer.append('r')
      case '\t' => writer.append('t')
      case possibleUnicode =>
        writer.append('u').append(String.format("%04x", Integer.valueOf(possibleUnicode.toInt)))
    }
  }

  private[this] def printJsonString(writer: Appendable)(jsonString: String): Unit = {
    writer.append('"')

    var i = 0
    var offset = 0

    while (i < jsonString.length) {
      val c = jsonString.charAt(i)
      if (!Printer.isNormalChar(c)) {
        writer.append(jsonString, offset, i)
        printEscapedChar(writer)(c)
        offset = i + 1
      }

      i += 1
    }

    if (offset < i) writer.append(jsonString, offset, i)
    writer.append('"')
  }

  private[this] final def printJsonAtDepth(writer: Appendable)(json: Json, depth: Int): Unit = {
    if (json.isNull) writer.append(nullText) else (json: @unchecked) match {
      case Json.JString(s) => printJsonString(writer)(s)
      case Json.JNumber(n) => writer.append(n.toString)
      case Json.JBoolean(b) => if (b) writer.append("true") else writer.append("false")
      case Json.JObject(o) =>
        val p = pieces(depth)
        writer.append(p.lBraces)
        val items = if (preserveOrder) o.toList else o.toMap
        var first = true

        val fieldIterator = items.iterator

        while (fieldIterator.hasNext) {
          val (key, value) = fieldIterator.next()
          if (!dropNullKeys || !value.isNull) {
            if (!first) writer.append(p.objectCommas)
            printJsonString(writer)(key)
            writer.append(p.colons)
            printJsonAtDepth(writer)(value, depth + 1)
            first = false
          }
        }
        writer.append(p.rBraces)
      case Json.JArray(a) =>
        val p = pieces(depth)
        val len = a.length

        if (len == 0) writer.append(p.lrEmptyBrackets) else {
          writer.append(p.lBrackets)
          printJsonAtDepth(writer)(a(0), depth + 1)

          var i = 1

          while (i < len) {
            writer.append(p.arrayCommas)
            printJsonAtDepth(writer)(a(i), depth + 1)
            i += 1
          }

          writer.append(p.rBrackets)
        }
    }
  }

  /**
   * Returns a string representation of a pretty-printed JSON value.
   */
  final def pretty(json: Json): String = {
    val writer = new StringBuilder()

    printJsonAtDepth(writer)(json, 0)

    writer.toString
  }

  final def prettyBytes(json: Json): Array[Byte] = {
    val bytes = new ByteArrayOutputStream()
    val writer = new BufferedWriter(new OutputStreamWriter(bytes, "UTF-8"))

    printJsonAtDepth(writer)(json, 0)

    writer.close()
    bytes.toByteArray
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

  private[circe] final def isNormalChar(c: Char): Boolean = (c: @switch) match {
    case '\\' => false
    case '"'  => false
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
