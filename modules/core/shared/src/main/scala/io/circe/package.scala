package io

package object circe {
  type Codec[A] = Encoder[A] with Decoder[A]
}
