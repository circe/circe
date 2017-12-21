package io.circe

/**
  *
  * @author Lorand Szakacs, lsz@lorandszakacs.com
  * @since 21 Dec 2017
  *
  */
object Codec {
  def apply[A](implicit instance: Codec[A]): Codec[A] = instance

  def instance[A](encoder: Encoder[A], decoder: Decoder[A]): Codec[A] =
    new Encoder[A] with Decoder[A] {
      private val enc = encoder
      private val dec = decoder
      override def apply(a: A): Json = enc(a)
      override def apply(c: HCursor): Decoder.Result[A] = dec(c)
    }
}
