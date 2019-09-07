package io.circe

import cats.{ Eq, Show, Semigroup}
import cats.data.NonEmptyList

/**
 * The base exception type for both decoding and parsing errors.
 */
sealed abstract class Error extends Exception {
  final override def fillInStackTrace(): Throwable = this
}

/**
 * A convenience exception type for aggregating one or more decoding or parsing
 * errors.
 */
final case class Errors(errors: NonEmptyList[Error]) extends Exception {
  def toList: List[Error] = errors.head :: errors.tail

  override def fillInStackTrace(): Throwable = this
}

/**
 * An exception representing a parsing failure and wrapping the exception
 * provided by the parsing library.
 */
final case class ParsingFailure(message: String, underlying: Throwable) extends Error {
  final override def getMessage: String = message
}

object ParsingFailure {
  implicit final val eqParsingFailure: Eq[ParsingFailure] = Eq.instance {
    case (ParsingFailure(m1, t1), ParsingFailure(m2, t2)) => m1 == m2 && t1 == t2
  }

  implicit final val showParsingFailure: Show[ParsingFailure] = Show.show { failure =>
    s"ParsingFailure: ${failure.message}"
  }
}

/**
 * An exception representing a decoding failure and (lazily) capturing the
 * decoding history resulting in the failure.
 */
sealed trait DecodingFailure extends Error{
  val message : String
}

sealed abstract class SingleDecodingFailure(val message: String) extends DecodingFailure {
  def history: List[CursorOp]

  final override def getMessage: String =
    if (history.isEmpty) message else s"$message: ${history.mkString(",")}"

  final def copy(message: String = message, history: => List[CursorOp] = history): SingleDecodingFailure = {
    def newHistory = history
    new SingleDecodingFailure(message) {
      final lazy val history: List[CursorOp] = newHistory
    }
  }

  final def withMessage(message: String): SingleDecodingFailure = copy(message = message)

  override final def toString: String = s"SingleDecodingFailure($message, $history)"
  override final def equals(that: Any): Boolean = that match {
    case other: DecodingFailure => DecodingFailure.eqDecodingFailure.eqv(this, other)
    case _                      => false
  }
  override final def hashCode: Int = message.hashCode
}

object SingleDecodingFailure {
  def unapply(error: Error): Option[(String, List[CursorOp])] = error match {
      case ParsingFailure(_, _)   => None
      case _ : AggregatedDecodingFailure => None
      case other: SingleDecodingFailure => Some((other.message, other.history))
    }
}

sealed abstract class AggregatedDecodingFailure(val failures: NonEmptyList[DecodingFailure], val optionalMessage: Option[String]) extends DecodingFailure{

  val message = optionalMessage.getOrElse(s"${failures.map(_.message)}")

  final def withMessage(message: String): AggregatedDecodingFailure = new AggregatedDecodingFailure(
      failures, Some(message)
  ){}
}

object AggregatedDecodingFailure {
  def unapply(error: Error): Option[NonEmptyList[DecodingFailure]] = error match {
      case ParsingFailure(_, _)   => None
      case _ : SingleDecodingFailure => None
      case other: AggregatedDecodingFailure => Some(other.failures)
    }
}

object DecodingFailure {
  def apply(message: String, ops: => List[CursorOp]): DecodingFailure = new SingleDecodingFailure(message) {
    final lazy val history: List[CursorOp] = ops
  }

  def combine(x: DecodingFailure, y: DecodingFailure): DecodingFailure = 
    new AggregatedDecodingFailure(NonEmptyList.of(x, y), None){}

  def fromThrowable(t: Throwable, ops: => List[CursorOp]): DecodingFailure = t match {
    case (d: DecodingFailure) => d
    case other =>
      val sw = new java.io.StringWriter
      val pw = new java.io.PrintWriter(sw)
      other.printStackTrace(pw)
      DecodingFailure(sw.toString, ops)
  }

  implicit final val eqDecodingFailure: Eq[DecodingFailure] = Eq.instance {
    case (SingleDecodingFailure(m1, h1), SingleDecodingFailure(m2, h2)) => m1 == m2 && CursorOp.eqCursorOpList.eqv(h1, h2)
    case (AggregatedDecodingFailure(e1), AggregatedDecodingFailure(e2)) => e1 == e2
    case (_, _) => false
  }

  implicit val semigroupDecodingFailure: Semigroup[DecodingFailure] = new Semigroup[DecodingFailure]{
    def combine(x: DecodingFailure, y: DecodingFailure): DecodingFailure = 
      DecodingFailure.combine(x,y)
  }

  /**
   * Creates compact, human readable string representations for DecodingFailure
   * Cursor history is represented as JS style selections, i.e. ".foo.bar[3]"
   */
  implicit final val showDecodingFailure: Show[DecodingFailure] = Show.show { 
    case e: SingleDecodingFailure =>
      val path = CursorOp.opsToPath(e.history)
      s"SingleDecodingFailure at ${path}: ${e.message}"
    case e: AggregatedDecodingFailure => 
      s"AggregatedDecodingFailure: ${e.message}"
  }

}

object Error {
  implicit final val eqError: Eq[Error] = Eq.instance {
    case (pf1: ParsingFailure, pf2: ParsingFailure)   => ParsingFailure.eqParsingFailure.eqv(pf1, pf2)
    case (df1: DecodingFailure, df2: DecodingFailure) => DecodingFailure.eqDecodingFailure.eqv(df1, df2)
    case (_, _)                                       => false
  }

  implicit final val showError: Show[Error] = Show.show {
    case pf: ParsingFailure  => ParsingFailure.showParsingFailure.show(pf)
    case df: DecodingFailure => DecodingFailure.showDecodingFailure.show(df)
  }
}
