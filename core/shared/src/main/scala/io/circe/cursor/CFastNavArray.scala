package io.circe.cursor

import cats.Functor
import io.circe.{ Context, Cursor, Json }

private[circe] case class CFastNavArray(
  values: IndexedSeq[Json],
  focusIndex: Int,
  parent: Cursor
) extends Cursor { self =>
  private[circe] override def normalize: Cursor = CArray(
    focus,
    parent,
    false,
    values.take(focusIndex).reverse.toList,
    values.drop(focusIndex + 1).toList
  )

  def focus: Json = values(focusIndex)
  def context: List[Context] = Context.inArray(focus, focusIndex) :: parent.context
  def up: Option[Cursor] = Some(parent)
  def delete: Option[Cursor] = normalize.delete

  def withFocus(f: Json => Json): Cursor = normalize.withFocus(f)
  def withFocusM[F[_]](f: Json => F[Json])(implicit F: Functor[F]): F[Cursor] =
    normalize.withFocusM(f)

  override def lefts: Option[List[Json]] = Some(values.take(focusIndex).reverse.toList)
  override def rights: Option[List[Json]] = Some(values.drop(focusIndex + 1).toList)

  override def left: Option[Cursor] = if (focusIndex == 0) None else Some(
    copy(focusIndex = focusIndex - 1)
  )

  override def right: Option[Cursor] = if (focusIndex == values.size - 1) None else Some(
    copy(focusIndex = focusIndex + 1)
  )

  override def first: Option[Cursor] = if (values.isEmpty) None else Some(copy(focusIndex = 0))
  override def last: Option[Cursor] = if (values.isEmpty) None else Some(
    copy(focusIndex = values.size - 1)
  )

  override def deleteGoLeft: Option[Cursor] = normalize.deleteGoLeft
  override def deleteGoRight: Option[Cursor] = normalize.deleteGoRight
  override def deleteGoFirst: Option[Cursor] = normalize.deleteGoFirst
  override def deleteGoLast: Option[Cursor] = normalize.deleteGoLast
  override def deleteLefts: Option[Cursor] = normalize.deleteLefts
  override def deleteRights: Option[Cursor] = normalize.deleteRights
  override def setLefts(js: List[Json]): Option[Cursor] = normalize.setLefts(js)
  override def setRights(js: List[Json]): Option[Cursor] = normalize.setRights(js)
}
