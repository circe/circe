import io.circe._

/**
 * A type class that provides back and forth conversion between values of type `A`
 * and the [[Json]] format. Must obey the laws defined in [[io.circe.tests.CodecLaws]].
 */
trait Codec[A] extends Encoder[A] with Decoder[A]

object Codec {
  def apply[A](implicit instance: Codec[A]): Codec[A] = instance

  implicit def fromEncoderDecoder[A](implicit e: Encoder[A], d: Decoder[A]): Codec[A] =
    new Codec[A] {
      def apply(c: HCursor): Decoder.Result[A] = d(c)
      def apply(a: A): Json = e(a)
    }
}
