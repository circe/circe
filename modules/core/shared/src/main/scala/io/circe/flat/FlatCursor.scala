package io.circe.flat

import io.circe.{ ACursor, CursorOp, HCursor, Json }
import java.util.{ List => JList }
import scala.annotation.switch

final class FlatCursor private (
  private[this] val content: String,
  private[this] val blocks: Array[Short], //Array[Array[Int]],
  //private[this] val blockSize: Int,
  private[this] val current: Int,
  private[this] val last: Int,
  lastCursor: HCursor,
  lastOp: CursorOp
) extends HCursor(lastCursor, lastOp) {
  //@inline private[this] def getBlock(element: Int): Array[Int] = this.blocks((element * 4) / this.blockSize)
  //@inline private[this] def getBlockIndex(element: Int): Int = (element * 4) % this.blockSize

  def delete: ACursor = Predef.???
  def field(k: String): ACursor = Predef.???
  def first: ACursor = Predef.???
  def left: ACursor = Predef.???
  def right: ACursor = {
    if (this.last > 0 && this.current < this.last) {
      //val block = this.getBlock(this.current)
      //val index = this.getBlockIndex(this.current)
      val nested = blocks(this.current + Overlay.NestedOffset) * 4
      //System.out.println(last + " " + current)

      return new FlatCursor(
        content,
        blocks,
        //blockSize,
        this.current + nested + 4,
        this.last,
        this,
        CursorOp.MoveRight
      )
    } else {
      fail(CursorOp.MoveRight)
    }
  }

  override final def asNumberString: String = {
    //val block = this.getBlock(this.current)
    //val index = this.getBlockIndex(this.current)
    //val current4 = this.current * 4

    if (blocks(current) == Overlay.NumberType) {
      val start = blocks(current + Overlay.StartOffset)
      val end = blocks(current + Overlay.EndOffset)
      this.content.substring(start, end + 1)
    } else null
  }

  override final def isTrue: Boolean = {
    //val block = this.getBlock(this.current)
    //val index = this.getBlockIndex(this.current)

    blocks(this.current) == Overlay.TrueType
  }

  override final def isFalse: Boolean = {
    //val block = this.getBlock(this.current)
    //val index = this.getBlockIndex(this.current)

    blocks(this.current) == Overlay.FalseType
  }

  override def asBoolean: java.lang.Boolean = {
    val jsonType = blocks(this.current)

    if (jsonType == Overlay.TrueType) {
      java.lang.Boolean.valueOf(true)
    } else if (jsonType == Overlay.FalseType) {
      java.lang.Boolean.valueOf(false)
    } else {
      null
    }
  }

  def up: ACursor = Predef.???

  def addOp(cursor: HCursor, op: CursorOp): HCursor = new FlatCursor(content, blocks, current, -1, cursor, op)
  def replace(newValue: Json, cursor: HCursor, op: CursorOp): HCursor = Predef.???
  def value: Json = this.toJson(current)

  override def downArray: ACursor = {
    //val block = this.getBlock(this.current)
    //val index = this.getBlockIndex(this.current)
    val jsonType = blocks(this.current)
    val nested = blocks(this.current + Overlay.NestedOffset) * 4

    if (jsonType == Overlay.ArrayType && nested > 0) {
      return new FlatCursor(
        content,
        blocks,
        this.current + 4,
        this.current + nested,
        this,
        CursorOp.DownArray
      )
    } else {
      fail(CursorOp.DownArray)
    }
  }

  /*import java.util.HashMap

  lazy val indices: HashMap[String, Int] ={
    val current4 = this.current * 4
    val jsonType = blocks(current4)

    if (jsonType == Overlay.ObjectType) {
      var next = this.current + 1
      val last = this.current + blocks(current4 + Overlay.NestedOffset)
      val map = new HashMap[String, Int]

      while (next <= last) {
        val next4 = next * 4

        val start = blocks(next4 + Overlay.StartOffset) + 1
        val end = blocks(next4 + Overlay.EndOffset)
        map.put(this.content.substring(start, end), next + 1)

        next += blocks((next + 1) * 4 + Overlay.NestedOffset) + 2
      }
      map
    } else {
      null
    }
  }*/

  override def downField(k: String): ACursor = {
    /*val map = this.indices
    //System.out.println(map.size)

    if (map.ne(null)) {
      if (map.containsKey(k)) {
        val next = map.get(k)

        return new FlatCursor(
          content,
          blocks,
          next,
          -1,
          this,
          CursorOp.DownField(k)
        )
      }
    }
    fail(CursorOp.DownField(k))*/

    //val block = this.getBlock(this.current)
    //val index = this.getBlockIndex(this.current)
    //val current4 = this.current
    val jsonType = blocks(current)

    if (jsonType == Overlay.ObjectType) {
      var next = this.current + 4
      val last = this.current + blocks(current + Overlay.NestedOffset) * 4

      while (next <= last) {
        //val next4 = next
        //val nextBlock = this.getBlock(next)
        //val nextIndex = this.getBlockIndex(next)

        val start = blocks(next + Overlay.StartOffset) + 1
        val end = blocks(next + Overlay.EndOffset)

        if (end - start == k.length) {
          var i = 0
          var ci = start
          var failed = false

          while (ci < end && !failed) {
            if (this.content.charAt(ci) != k.charAt(i)) {
              failed = true
              //import scala.util.control.Breaks._
              //break
            }
            i += 1
            ci += 1
          }
          if (!failed) {
            return new FlatCursor(
              content,
              blocks,
              next + 4,
              -1,
              this,
              CursorOp.DownField(k)
            )
          }
        }

        //val valueBlock = this.getBlock(next + 1)
        //val valueIndex = this.getBlockIndex(next + 1)

        next += (blocks((next + 4) + Overlay.NestedOffset) * 4 + 8)
      }
      fail(CursorOp.DownField(k))
    } else {
      fail(CursorOp.DownField(k))
    }
  }

  private[this] def getString(element: Int): String = {
    //val block = this.getBlock(element)
    //val index = this.getBlockIndex(element)

    val jsonType = blocks(element)
    val start = blocks(element + Overlay.StartOffset) + 1
    val end = blocks(element + Overlay.EndOffset)

    if (jsonType == Overlay.StringType) {
      this.content.subSequence(start, end).toString
    } else if (jsonType == Overlay.StringEscapedType) {
      val result = new java.lang.StringBuilder(end - start)
      var i = start

      while (i < end) {
        if (this.content.charAt(i) == '\\') {
          i += 1
          (this.content.charAt(i): @switch) match {
            case '\\' => result.append('\\')
            case '/'  => result.append('/')
            case '"'  => result.append('"')
            case 'b'  => result.append('\b')
            case 'f'  => result.append('\f')
            case 'n'  => result.append('\n')
            case 'r'  => result.append('\r')
            case 't'  => result.append('\t')
            case 'u' =>
              result.append(Character.toChars(Integer.parseInt(this.content.substring(i + 1, i + 5).toString, 16)));
              i += 4
          }
        } else {
          result.append(this.content.charAt(i))
        }

        i += 1
      }

      result.toString
    } else {
      null
    }
  }

  private[this] def toJson(element: Int): io.circe.Json = {
    import io.circe.{ Json, JsonNumber }

    //val block = this.getBlock(element)
    //val index = this.getBlockIndex(element)

    val jsonType = blocks(element)
    //System.out.println(jsonType)

    (jsonType: @switch) match {
      case Overlay.NullType  => Json.Null
      case Overlay.TrueType  => Json.True
      case Overlay.FalseType => Json.False
      case Overlay.StringType | Overlay.StringEscapedType =>
        Json.fromString(this.getString(element))
      case Overlay.NumberType =>
        val start = blocks(element + Overlay.StartOffset)
        val end = blocks(element + Overlay.EndOffset)
        Json.fromJsonNumber(JsonNumber.fromDecimalStringUnsafe(this.content.substring(start, end + 1).toString))
      case Overlay.ArrayType =>
        val result = Vector.newBuilder[Json]
        val nested = blocks(element + Overlay.NestedOffset) * 4
        var next = element + 4

        while (next <= element + nested) {
          //val nextBlock = this.getBlock(next)
          //val nextIndex = this.getBlockIndex(next)
          result += toJson(next)
          next += blocks(next + Overlay.NestedOffset) * 4 + 4
        }

        Json.fromValues(result.result())
      case Overlay.ObjectType =>
        val result = Vector.newBuilder[(String, Json)]
        val nested = blocks(element + Overlay.NestedOffset) * 4
        var next = element + 4

        while (next <= element + nested) {
          val key = this.getString(next)
          //val nextBlock = this.getBlock(next + 1)
          //val nextIndex = this.getBlockIndex(next + 1)
          result += ((key, toJson(next + 4)))
          next += blocks((next + 4) + Overlay.NestedOffset) * 4 + 8
        }

        Json.fromFields(result.result())
      case _ => Json.Null
    }
  }
}

object FlatCursor {
  def parse(input: String): Either[ParseFailure, FlatCursor] = {
    val overlay = Overlay.parseUnsafe(input)

    val failure = overlay.getFailure

    if (failure.eq(null)) {
      val blocks = overlay.getBlocksUnsafe
      val count = blocks.size
      val size = overlay.blockSize
      val newArray = new Array[Short](count * size)
      var i = 0

      while (i < count) {
        System.arraycopy(blocks.get(i), 0, newArray, i * size, size)

        i += 1
      }

      Right(new FlatCursor(input, newArray, 0, -1, null, null))
    } else {
      Left(failure)
    }
  }

  def decode[A](input: String)(implicit decodeA: io.circe.Decoder[A]): Either[io.circe.Error, A] =
    parse(input) match {
      case Right(c)      => decodeA(c)
      case Left(failure) => Left(io.circe.ParsingFailure(failure.getMessage, failure))
    }
}
