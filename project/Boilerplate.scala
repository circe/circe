import sbt._

/**
 * Generate a range of boilerplate classes that would be tedious to write and maintain by hand.
 *
 * Copied, with some modifications, from
 * [[https://github.com/milessabin/shapeless/blob/master/project/Boilerplate.scala Shapeless]].
 *
 * @author Miles Sabin
 * @author Kevin Wright
 */
object Boilerplate {
  import scala.StringContext._

  implicit class BlockHelper(val sc: StringContext) extends AnyVal {
    def block(args: Any*): String = {
      val interpolated = sc.standardInterpolator(treatEscapes, args)
      val rawLines = interpolated.split('\n')
      val trimmedLines = rawLines.map(_.dropWhile(_.isWhitespace))
      trimmedLines.mkString("\n")
    }
  }

  val templates: Seq[Template] = Seq(
    GenTupleDecoders,
    GenTupleEncoders,
    GenProductDecoders,
    GenProductEncoders,
    GenProductCodecs
  )

  val testTemplates: Seq[Template] = Seq(
    GenTupleTests,
    GenProductTests
  )

  val header = "// auto-generated boilerplate"
  val maxArity = 22

  /**
   * Return a sequence of the generated files.
   *
   * As a side-effect, it actually generates them...
   */
  def gen(dir: File): Seq[File] = templates.map { template =>
    val tgtFile = template.filename(dir)
    IO.write(tgtFile, template.body)
    tgtFile
  }

  /**
   * Return a sequence of the generated test files.
   *
   * As a side-effect, it actually generates them...
   */
  def genTests(dir: File): Seq[File] = testTemplates.map { template =>
    val tgtFile = template.filename(dir)
    IO.write(tgtFile, template.body)
    tgtFile
  }

  class TemplateVals(val arity: Int) {
    val synTypes = (0 until arity).map(n => s"A$n")
    val synVals = (0 until arity).map(n => s"a$n")
    val `A..N` = synTypes.mkString(", ")
    val `a..n` = synVals.mkString(", ")
    val `_.._` = Seq.fill(arity)("_").mkString(", ")
    val `(A..N)` = if (arity == 1) "Tuple1[A0]" else synTypes.mkString("(", ", ", ")")
    val `(_.._)` = if (arity == 1) "Tuple1[_]" else Seq.fill(arity)("_").mkString("(", ", ", ")")
    val `(a..n)` = if (arity == 1) "Tuple1(a)" else synVals.mkString("(", ", ", ")")
  }

  /**
   * Blocks in the templates below use a custom interpolator, combined with post-processing to
   * produce the body.
   *
   * - The contents of the `header` val is output first
   * - Then the first block of lines beginning with '|'
   * - Then the block of lines beginning with '-' is replicated once for each arity,
   *   with the `templateVals` already pre-populated with relevant vals for that arity
   * - Then the last block of lines prefixed with '|'
   *
   * The block otherwise behaves as a standard interpolated string with regards to variable
   * substitution.
   */
  trait Template {
    def filename(root: File): File
    def content(tv: TemplateVals): String
    def range: IndexedSeq[Int] = 1 to maxArity
    def body: String = {
      val headerLines = header.split('\n')
      val raw = range.map(n => content(new TemplateVals(n)).split('\n').filterNot(_.isEmpty))
      val preBody = raw.head.takeWhile(_.startsWith("|")).map(_.tail)
      val instances = raw.flatMap(_.filter(_.startsWith("-")).map(_.tail))
      val postBody = raw.head.dropWhile(_.startsWith("|")).dropWhile(_.startsWith("-")).map(_.tail)
      (headerLines ++ preBody ++ instances ++ postBody).mkString("\n")
    }
  }

  object GenTupleDecoders extends Template {
    override def range: IndexedSeq[Int] = 1 to maxArity

    def filename(root: File): File = root / "io" / "circe" / "TupleDecoders.scala"

    def content(tv: TemplateVals): String = {
      import tv._

      val instances = synTypes.map(tpe => s"decode$tpe: Decoder[$tpe]").mkString(", ")
      val applied = synTypes.zipWithIndex.map {
        case (tpe, n) => s"decode$tpe.tryDecode(c.downN($n))"
      }.mkString(", ")

      val accumulatingApplied = synTypes.zipWithIndex.map {
        case (tpe, n) => s"decode$tpe.tryDecodeAccumulating(c.downN($n))"
      }.mkString(", ")

      val result =
        if (arity == 1) s"Decoder.resultInstance.map($applied)(Tuple1(_))"
        else s"Decoder.resultInstance.tuple$arity($applied)"

      val accumulatingResult =
        if (arity == 1) s"$accumulatingApplied.map(Tuple1(_))"
        else s"Decoder.accumulatingResultInstance.tuple$arity($accumulatingApplied)"

      block"""
        |package io.circe
        |
        |import cats.data.Validated
        |import io.circe.DecodingFailure.Reason.WrongTypeExpectation
        |
        |private[circe] trait TupleDecoders {
        -  /**
        -   * @group Tuple
        -   */
        -  implicit final def decodeTuple$arity[${`A..N`}](implicit $instances): Decoder[${`(A..N)`}] =
        -    new Decoder[${`(A..N)`}] {
        -      final def apply(c: HCursor): Decoder.Result[${`(A..N)`}] = c.value match {
        -        case Json.JArray(values) if values.lengthCompare($arity) == 0 => $result
        -        case json => Left(DecodingFailure(WrongTypeExpectation("array", json), c.history))
        -      }
        -
        -      override final def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[${`(A..N)`}] =
        -        c.value match {
        -          case Json.JArray(values) if values.lengthCompare($arity) == 0 => $accumulatingResult
        -          case json => Validated.invalidNel(
        -            DecodingFailure(WrongTypeExpectation("array", json), c.history))
        -        }
        -    }
        |}
      """
    }
  }

  object GenTupleEncoders extends Template {
    override def range: IndexedSeq[Int] = 1 to maxArity

    def filename(root: File): File = root / "io" / "circe" / "TupleEncoders.scala"

    def content(tv: TemplateVals): String = {
      import tv._

      val instances = synTypes.map(tpe => s"encode$tpe: Encoder[$tpe]").mkString(", ")
      val applied = synTypes.zipWithIndex.map {
        case (tpe, n) => s"encode$tpe(a._${n + 1})"
      }.mkString(", ")

      block"""
        |package io.circe
        |
        |private[circe] trait TupleEncoders {
        -  /**
        -   * @group Tuple
        -   */
        -  implicit final def encodeTuple$arity[${`A..N`}](implicit $instances): Encoder.AsArray[${`(A..N)`}] =
        -    new Encoder.AsArray[${`(A..N)`}] {
        -      final def encodeArray(a: ${`(A..N)`}): Vector[Json] = Vector($applied)
        -    }
        |}
      """
    }
  }

  object GenTupleTests extends Template {
    override def range: IndexedSeq[Int] = 2 to maxArity

    def filename(root: File): File = root / "io" / "circe" / "TupleCodecSuite.scala"

    def content(tv: TemplateVals): String = {
      import tv._

      val tupleTypeList = synTypes.map(_ => "Int").mkString(",")
      val tupleType = s"($tupleTypeList)"

      block"""
        |package io.circe
        |
        |import cats.kernel.instances.int._
        |import cats.kernel.instances.tuple._
        |import io.circe.testing.CodecTests
        |import io.circe.tests.CirceMunitSuite
        |
        |class TupleCodecSuite extends CirceMunitSuite {
        |  checkAll("Codec[Tuple1[Int]]", CodecTests[Tuple1[Int]].codec)
        -  checkAll("Codec[$tupleType]", CodecTests[$tupleType].codec)
        |}
      """
    }
  }

  object GenProductDecoders extends Template {
    override def range: IndexedSeq[Int] = 1 to maxArity

    def filename(root: File): File = root / "io" / "circe" / "ProductDecoders.scala"

    def content(tv: TemplateVals): String = {
      import tv._

      val instances = synTypes.map(tpe => s"decode$tpe: Decoder[$tpe]").mkString(", ")
      val memberNames = synTypes.map(tpe => s"name$tpe: String").mkString(", ")

      val results = synTypes.map(tpe => s"c.get[$tpe](name$tpe)(decode$tpe)").mkString(", ")

      val accumulatingResults =
        synTypes.map(tpe => s"decode$tpe.tryDecodeAccumulating(c.downField(name$tpe))").mkString(",")

      val result =
        if (arity == 1) s"Decoder.resultInstance.map($results)(f)" else s"Decoder.resultInstance.map$arity($results)(f)"

      val accumulatingResult =
        if (arity == 1) s"$accumulatingResults.map(f)"
        else s"Decoder.accumulatingResultInstance.map$arity($accumulatingResults)(f)"

      block"""
        |package io.circe
        |
        |private[circe] trait ProductDecoders {
        -  /**
        -   * @group Product
        -   */
        -  final def forProduct$arity[Target, ${`A..N`}]($memberNames)(f: (${`A..N`}) => Target)(implicit
        -    $instances
        -  ): Decoder[Target] =
        -    new Decoder[Target] {
        -      final def apply(c: HCursor): Decoder.Result[Target] = $result
        -
        -      override final def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[Target] =
        -        $accumulatingResult
        -    }
        |}
      """
    }
  }

  object GenProductEncoders extends Template {
    override def range: IndexedSeq[Int] = 1 to maxArity

    def filename(root: File): File = root / "io" / "circe" / "ProductEncoders.scala"

    def content(tv: TemplateVals): String = {
      import tv._

      val instances = synTypes.map(tpe => s"encode$tpe: Encoder[$tpe]").mkString(", ")
      val memberNames = synTypes.map(tpe => s"name$tpe: String").mkString(", ")
      val kvs =
        if (arity == 1) s"(name${synTypes.head}, encode${synTypes.head}(members))"
        else {
          synTypes.zipWithIndex.map {
            case (tpe, i) => s"(name$tpe, encode$tpe(members._${i + 1}))"
          }.mkString(", ")
        }
      val outputType = if (arity != 1) s"Product$arity[${`A..N`}]" else `A..N`

      block"""
        |package io.circe
        |
        |private[circe] trait ProductEncoders {
        -  /**
        -   * @group Product
        -   */
        -  final def forProduct$arity[Source, ${`A..N`}]($memberNames)(f: Source => $outputType)(implicit
        -    $instances
        -  ): Encoder.AsObject[Source] =
        -    new Encoder.AsObject[Source] {
        -      final def encodeObject(a: Source): JsonObject = {
        -        val members = f(a)
        -        JsonObject.fromIterable(Vector($kvs))
        -      }
        -    }
        |}
      """
    }
  }

  object GenProductCodecs extends Template {
    override def range: IndexedSeq[Int] = 1 to maxArity

    def filename(root: File): File = root / "io" / "circe" / "ProductCodecs.scala"

    def content(tv: TemplateVals): String = {
      import tv._

      val decoderInstances = synTypes.map(tpe => s"decode$tpe: Decoder[$tpe]").mkString(", ")
      val encoderInstances = synTypes.map(tpe => s"encode$tpe: Encoder[$tpe]").mkString(", ")

      val memberNames = synTypes.map(tpe => s"name$tpe: String").mkString(", ")

      val results = synTypes.map(tpe => s"c.get[$tpe](name$tpe)(decode$tpe)").mkString(", ")

      val accumulatingResults =
        synTypes.map(tpe => s"decode$tpe.tryDecodeAccumulating(c.downField(name$tpe))").mkString(",")

      val result =
        if (arity == 1) s"Decoder.resultInstance.map($results)(f)" else s"Decoder.resultInstance.map$arity($results)(f)"

      val accumulatingResult =
        if (arity == 1) s"$accumulatingResults.map(f)"
        else s"Decoder.accumulatingResultInstance.map$arity($accumulatingResults)(f)"

      val kvs =
        if (arity == 1) s"(name${synTypes.head}, encode${synTypes.head}(members))"
        else {
          synTypes.zipWithIndex.map {
            case (tpe, i) => s"(name$tpe, encode$tpe(members._${i + 1}))"
          }.mkString(", ")
        }
      val outputType = if (arity != 1) s"Product$arity[${`A..N`}]" else `A..N`

      block"""
        |package io.circe
        |
        |private[circe] trait ProductCodecs {
        -  /**
        -   * @group Product
        -   */
        -  final def forProduct$arity[A, ${`A..N`}]($memberNames)(f: (${`A..N`}) => A)(g: A => $outputType)(implicit
        -    $decoderInstances,
        -    $encoderInstances
        -  ): Codec.AsObject[A] =
        -    new Codec.AsObject[A] {
        -      final def apply(c: HCursor): Decoder.Result[A] = $result
        -
        -      override final def decodeAccumulating(c: HCursor): Decoder.AccumulatingResult[A] =
        -        $accumulatingResult
        -
        -      final def encodeObject(a: A): JsonObject = {
        -        val members = g(a)
        -        JsonObject.fromIterable(Vector($kvs))
        -      }
        -    }
        |}
      """
    }
  }

  object GenProductTests extends Template {
    override def range: IndexedSeq[Int] = 1 to maxArity

    def filename(root: File): File = root / "io" / "circe" / "ProductCodecSuite.scala"

    def content(tv: TemplateVals): String = {
      import tv._

      val members = (0 until arity).map(i => s"s$i: String").mkString(", ")
      val memberNames = (0 until arity).map(i => "\"" + s"s$i" + "\"").mkString(", ")
      val memberTypes = (0 until arity).map(_ => "String").mkString(", ")

      val memberVariableNames = (0 until arity).map(i => s"s$i").mkString(", ")
      val memberArbitraryItems = (0 until arity).map(i => s"s$i <- Arbitrary.arbitrary[String]").mkString("; ")

      block"""
        |package io.circe
        |
        |import cats.kernel.Eq
        |import io.circe.testing.CodecTests
        |import io.circe.tests.CirceMunitSuite
        |import org.scalacheck.Arbitrary
        |
        |class ProductCodecSuite extends CirceMunitSuite {
        -  case class Cc$arity($members)
        -  object Cc$arity {
        -    implicit val eqCc$arity: Eq[Cc$arity] = Eq.fromUniversalEquals
        -    implicit val arbitraryCc$arity: Arbitrary[Cc$arity] = Arbitrary(
        -      for { $memberArbitraryItems } yield Cc$arity($memberVariableNames)
        -    )
        -    private def toTuple(cc: Cc$arity): ($memberTypes) = cc match {
        -      case Cc$arity($memberVariableNames) => ($memberVariableNames)
        -    }
        -    implicit val encodeCc$arity: Encoder[Cc$arity] =
        -      Encoder.forProduct$arity($memberNames)(toTuple)
        -    implicit val decodeCc$arity: Decoder[Cc$arity] =
        -      Decoder.forProduct$arity($memberNames)(Cc$arity.apply)
        -    val codecForCc$arity: Codec[Cc$arity] =
        -      Codec.forProduct$arity($memberNames)(Cc$arity.apply)(toTuple)
        -  }
        -  checkAll("Codec[Cc$arity]", CodecTests[Cc$arity].unserializableCodec)
        -  checkAll(
        -    "Codec[Cc$arity] via Codec",
        -    CodecTests[Cc$arity](Cc$arity.codecForCc$arity, Cc$arity.codecForCc$arity).unserializableCodec
        -  )
        -  checkAll(
        -    "Codec[Cc$arity] via Decoder and Codec",
        -    CodecTests[Cc$arity](Cc$arity.decodeCc$arity, Cc$arity.codecForCc$arity).unserializableCodec
        -  )
        -  checkAll(
        -    "Codec[Cc$arity] via Encoder and Codec",
        -    CodecTests[Cc$arity](Cc$arity.codecForCc$arity, Cc$arity.encodeCc$arity).unserializableCodec
        -  )
        |}
      """
    }
  }
}
