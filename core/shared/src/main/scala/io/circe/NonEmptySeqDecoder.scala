package io.circe

import scala.collection.generic.CanBuildFrom

private[circe] abstract class NonEmptySeqDecoder[A, C[_], S](implicit
  decodeA: Decoder[A],
  cbf: CanBuildFrom[Nothing, A, C[A]]
) extends Decoder[S] {
  protected def create: (A, C[A]) => S
  private[this] final val decodeCA: Decoder[C[A]] = Decoder.decodeCanBuildFrom[A, C](decodeA, cbf)

  final def apply(c: HCursor): Decoder.Result[S] = {
    val arr = c.downArray

    decodeA.tryDecode(arr) match {
      case Right(head) =>
        decodeCA.tryDecode(arr.delete) match {
          case Right(tail) => Right(create(head, tail))
          case l @ Left(_) => l.asInstanceOf[Decoder.Result[S]]
        }
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[S]]
    }
  }

  override final private[circe] def decodeAccumulating(
    c: HCursor
  ): AccumulatingDecoder.Result[S] = {
    val arr = c.downArray

    AccumulatingDecoder.resultInstance.map2(
      decodeA.tryDecodeAccumulating(arr),
      decodeCA.tryDecodeAccumulating(arr.delete)
    )(create)
  }
}
