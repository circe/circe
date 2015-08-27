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
    GenTupleEncoders
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

  class TemplateVals(val arity: Int) {
    val synTypes = (0 until arity).map(n => s"A$n")
    val synVals  = (0 until arity).map(n => s"a$n")
    val `A..N`   = synTypes.mkString(", ")
    val `a..n`   = synVals.mkString(", ")
    val `_.._`   = Seq.fill(arity)("_").mkString(", ")
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
   *   with the `templateVals` already pre-populated with relevant relevant vals for that arity
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

    def filename(root: File): File = root /  "io" / "circe" / "TupleDecoders.scala"

    def content(tv: TemplateVals): String = {
      import tv._

      val instances = synTypes.map(tpe => s"decode$tpe: Decoder[$tpe]").mkString(", ")
      val applied = synTypes.zipWithIndex.map {
        case (tpe, n) => s"decode$tpe(js($n))"
      }.mkString(", ")

      val result =
        if (arity == 1) s"$applied.map(Tuple1(_))" else s"resultApplicative.tuple$arity($applied)"

      block"""
        |package io.circe
        |
        |import cats.Applicative
        |import cats.data.Xor
        |
        |private[circe] trait TupleDecoders {
        |  private[this] val resultApplicative: Applicative[Decoder.Result] = implicitly
        |
        -  implicit def decodeTuple$arity[${`A..N`}](implicit $instances): Decoder[${`(A..N)`}] =
        -    Decoder.instance { c =>
        -      c.as[Vector[HCursor]].flatMap { js =>
        -        if (js.size == $arity) {
        -          $result
        -        } else Xor.left(DecodingFailure("${`(A..N)`}", c.history))
        -      }
        -    }
        |}
      """
    }
  }

  object GenTupleEncoders extends Template {
    override def range: IndexedSeq[Int] = 1 to maxArity

    def filename(root: File): File = root /  "io" / "circe" / "TupleEncoders.scala"

    def content(tv: TemplateVals): String = {
      import tv._

      val instances = synTypes.map(tpe => s"encode$tpe: Encoder[$tpe]").mkString(", ")
      val applied = synTypes.zipWithIndex.map {
        case (tpe, n) => s"encode$tpe(t._${ n + 1 })"
      }.mkString(", ")

      block"""
        |package io.circe
        |
        |private[circe] trait TupleEncoders {
        -  implicit def encodeTuple$arity[${`A..N`}](implicit $instances): Encoder[${`(A..N)`}] =
        -    Encoder.instance(t => Json.array($applied))
        |}
      """
    }
  }
}
