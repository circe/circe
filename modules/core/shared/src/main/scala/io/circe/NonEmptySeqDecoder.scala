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

    decodeA.tryDecode(arr).flatMap { head =>
      decodeCA.tryDecode(arr.delete).map { tail =>
        create(head, tail)
      }
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
