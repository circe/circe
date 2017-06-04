package io.circe

import java.lang.StringBuilder
import java.nio.{ ByteBuffer, CharBuffer }
import java.nio.charset.{ Charset, StandardCharsets }
import java.util.concurrent.CopyOnWriteArrayList
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
 * @param reuseWriters Determines whether the printer will reuse Appendables via thread-local
 *        storage.
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
  colonRight: String = "",
  reuseWriters: Boolean = false
) {
  private[this] final val openBraceText = "{"
  private[this] final val closeBraceText = "}"
  private[this] final val openArrayText = "["
  private[this] final val closeArrayText = "]"
  private[this] final val commaText = ","
  private[this] final val colonText = ":"

  private[this] final class PrintingFolder(writer: Appendable) extends Json.Folder[Unit] {
    private[this] var depth: Int = 0

    final def onNull: Unit = writer.append("null")
    final def onBoolean(value: Boolean): Unit = writer.append(java.lang.Boolean.toString(value))
    final def onNumber(value: JsonNumber): Unit = writer.append(value.toString)

    final def onString(value: String): Unit = {
      writer.append('"')

      var i = 0
      var offset = 0

      while (i < value.length) {
        val c = value.charAt(i)

        if (
          (c == '"' || c == '\\' || c == '\b' || c == '\f' || c == '\n' || c == '\r' || c == '\t') ||
          Character.isISOControl(c)
        ) {
          writer.append(value, offset, i)
          writer.append('\\')
          (c: @switch) match {
            case '"'  => writer.append('"')
            case '\\' => writer.append('\\')
            case '\b' => writer.append('b')
            case '\f' => writer.append('f')
            case '\n' => writer.append('n')
            case '\r' => writer.append('r')
            case '\t' => writer.append('t')
            case control =>
              writer.append(String.format("u%04x", Integer.valueOf(control.toInt)))
          }
          offset = i + 1
        }

        i += 1
      }

      if (offset < i) writer.append(value, offset, i)
      writer.append('"')
    }

    final def onArray(value: Vector[Json]): Unit = {
      val orig = depth
      val p = pieces(depth)

      if (value.isEmpty) writer.append(p.lrEmptyBrackets) else {
        val iterator = value.iterator

        writer.append(p.lBrackets)
        depth += 1
        iterator.next().foldWith(this)
        depth = orig

        while (iterator.hasNext) {
          writer.append(p.arrayCommas)
          depth += 1
          iterator.next().foldWith(this)
          depth = orig
        }

        writer.append(p.rBrackets)
      }
    }

    final def onObject(value: JsonObject): Unit = {
      val orig = depth
      val p = pieces(depth)
      val m = value.toMap

      writer.append(p.lBraces)
      val fields = if (preserveOrder) value.fields else value.fieldSet
      var first = true

      val fieldIterator = fields.iterator

      while (fieldIterator.hasNext) {
        val key = fieldIterator.next()
        val value = m(key)
        if (!dropNullKeys || !value.isNull) {
          if (!first) writer.append(p.objectCommas)
          onString(key)
          writer.append(p.colons)

          depth += 1
          value.foldWith(this)
          depth = orig
          first = false
        }
      }
      writer.append(p.rBraces)
    }
  }

  private[this] final def concat(left: String, text: String, right: String): String = {
    val builder = new StringBuilder()
    builder.append(left)
    builder.append(text)
    builder.append(right)
    builder.toString
  }

  private[this] final val pieces: Printer.PiecesAtDepth = if (indent.isEmpty) new Printer.ConstantPieces(
    Printer.Pieces(
      concat(lbraceLeft, openBraceText, lbraceRight),
      concat(rbraceRight, closeBraceText, rbraceLeft),
      concat(lbracketLeft, openArrayText, lbracketRight),
      concat(rbracketLeft, closeArrayText, rbracketRight),
      concat(openArrayText, lrbracketsEmpty, closeArrayText),
      concat(arrayCommaLeft, commaText, arrayCommaRight),
      concat(objectCommaLeft, commaText, objectCommaRight),
      concat(colonLeft, colonText, colonRight)
    )
  ) else new Printer.MemoizedPieces(indent) {
    final def compute(i: Int): Printer.Pieces = {
      val builder = new StringBuilder()

      addIndentation(builder, lbraceLeft, i)
      builder.append(openBraceText)
      addIndentation(builder, lbraceRight, i + 1)

      val lBraces = builder.toString

      builder.setLength(0)

      addIndentation(builder, rbraceLeft, i)
      builder.append(closeBraceText)
      addIndentation(builder, rbraceRight, i + 1)

      val rBraces = builder.toString

      builder.setLength(0)

      addIndentation(builder, lbracketLeft, i)
      builder.append(openArrayText)
      addIndentation(builder, lbracketRight, i + 1)

      val lBrackets = builder.toString

      builder.setLength(0)

      addIndentation(builder, rbracketLeft, i)
      builder.append(closeArrayText)
      addIndentation(builder, rbracketRight, i + 1)

      val rBrackets = builder.toString

      builder.setLength(0)

      builder.append(openArrayText)
      addIndentation(builder, lrbracketsEmpty, i)
      builder.append(closeArrayText)

      val lrEmptyBrackets = builder.toString

      builder.setLength(0)

      addIndentation(builder, arrayCommaLeft, i + 1)
      builder.append(commaText)
      addIndentation(builder, arrayCommaRight, i + 1)

      val arrayCommas = builder.toString

      builder.setLength(0)

      addIndentation(builder, objectCommaLeft, i + 1)
      builder.append(commaText)
      addIndentation(builder, objectCommaRight, i + 1)

      val objectCommas = builder.toString

      builder.setLength(0)

      addIndentation(builder, colonLeft, i + 1)
      builder.append(colonText)
      addIndentation(builder, colonRight, i + 1)

      val colons = builder.toString

      Printer.Pieces(lBraces, rBraces, lBrackets, rBrackets, lrEmptyBrackets, arrayCommas, objectCommas, colons)
    }
  }

  @transient
  private[this] final val stringWriter: ThreadLocal[StringBuilder] = new ThreadLocal[StringBuilder] {
    override final def initialValue: StringBuilder = new StringBuilder()
  }

  /**
   * Returns a string representation of a pretty-printed JSON value.
   */
  final def pretty(json: Json): String = {
    val writer = if (reuseWriters) {
      val w = stringWriter.get()
      w.setLength(0)
      w
    } else new StringBuilder()

    val folder = new PrintingFolder(writer)

    json.foldWith(folder)

    writer.toString
  }

  final def prettyByteBuffer(json: Json, cs: Charset): ByteBuffer = {
    val writer = new Printer.AppendableByteBuffer(cs)
    val folder = new PrintingFolder(writer)

    json.foldWith(folder)

    writer.toByteBuffer
  }

  final def prettyByteBuffer(json: Json): ByteBuffer =
    prettyByteBuffer(json, StandardCharsets.UTF_8)
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

  private[circe] abstract class PiecesAtDepth extends Serializable {
    def apply(i: Int): Pieces
  }

  private[circe] final class ConstantPieces(pieces: Pieces) extends PiecesAtDepth {
    def apply(i: Int): Pieces = pieces
  }

  private[circe] abstract class MemoizedPieces(indent: String) extends PiecesAtDepth {
    def compute(i: Int): Pieces

    private[this] final val known = new CopyOnWriteArrayList[Pieces](
      new Array[Pieces](maxMemoizationDepth)
    )

    protected[this] final def addIndentation(builder: StringBuilder, s: String, depth: Int): Unit = {
      val lastNewLineIndex = s.lastIndexOf('\n')

      if (lastNewLineIndex == -1) builder.append(s) else {
        builder.append(s, 0, lastNewLineIndex + 1)

        var i = 0

        while (i < depth) {
          builder.append(indent)
          i += 1
        }

        builder.append(s, lastNewLineIndex + 1, s.length)
      }
    }

    final def apply(i: Int): Pieces = if (i >= maxMemoizationDepth) compute(i) else {
      val res = known.get(i)

      if (res.ne(null)) res else {
        val tmp = compute(i)
        known.set(i, tmp)
        tmp
      }
    }
  }

  /**
   * Very bare-bones and fast [[Appendable]] that can produce a [[ByteBuffer]].
   *
   * The implementation is pretty much a regular growing char buffer that assumes all given
   * [[CharSequence]]s are just [[String]]s so both `toString` and `charAt` methods are cheap.
   */
  private[circe] final class AppendableByteBuffer(cs: Charset) extends Appendable {
    private[this] var index = 0
    private[this] var chars = new Array[Char](32)

    private[this] def ensureToFit(n: Int): Unit = {
      val required = index + n
      if (required > chars.length) {
        val copy = new Array[Char](math.max(required, chars.length * 2))
        System.arraycopy(chars, 0, copy, 0, index)
        chars = copy
      }
    }

    def append(csq: CharSequence): Appendable = {
      ensureToFit(csq.length)
      csq.toString.getChars(0, csq.length, chars, index)
      index += csq.length
      this
    }

    def append(csq: CharSequence, start: Int, end: Int): Appendable = {
      ensureToFit(end - start)
      var j = start
      while (j < end) {
        chars(index) = csq.charAt(j)
        j += 1
        index += 1
      }
      this
    }

    def append(c: Char): Appendable = {
      ensureToFit(1)
      chars(index) = c
      index += 1
      this
    }

    def toByteBuffer: ByteBuffer = cs.encode(CharBuffer.wrap(chars, 0, index))
  }
}
