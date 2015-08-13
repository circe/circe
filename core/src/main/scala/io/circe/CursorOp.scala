package io.circe

sealed abstract class CursorOp extends Product with Serializable {
  def isReattempt: Boolean
  def isNotReattempt: Boolean
  def succeeded: Boolean
  def failed: Boolean
  def op: Option[CursorOpElement]
}

object CursorOp {
  def ok(o: CursorOpElement): CursorOp = El(o, succeeded = true)
  def fail(o: CursorOpElement): CursorOp = El(o, succeeded = false)
  def reattempt: CursorOp = Reattempt

  private[circe] case object Reattempt extends CursorOp {
    def isReattempt: Boolean = true
    def isNotReattempt: Boolean = false
    def succeeded: Boolean = false
    def failed: Boolean = false
    def op: Option[CursorOpElement] = None
  }

  private[circe] case class El(o: CursorOpElement, succeeded: Boolean) extends CursorOp {
    def isReattempt: Boolean = false
    def isNotReattempt: Boolean = true
    def failed: Boolean = !succeeded
    def op: Option[CursorOpElement] = Some(o)
  }
}
