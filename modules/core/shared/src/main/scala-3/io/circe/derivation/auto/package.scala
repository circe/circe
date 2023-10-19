package io.circe.derivation.auto

import io.circe.{ Codec, Decoder, Encoder, Json }
import io.circe.derivation.{ Configuration, ConfiguredCodec, ConfiguredDecoder, ConfiguredEncoder }
import io.circe.`export`.Exported
import scala.deriving.Mirror

inline given exportedDecoder[A](using
  inline m: Mirror.Of[A],
  conf: Configuration = Configuration.default
): Exported[Decoder[A]] =
  new Exported(ConfiguredDecoder.derived[A])
inline given exportedEncoder[A](using
  inline m: Mirror.Of[A],
  conf: Configuration = Configuration.default
): Exported[Encoder.AsObject[A]] =
  new Exported(ConfiguredEncoder.derived[A])
inline given exportedCodec[A](using
  inline m: Mirror.Of[A],
  conf: Configuration = Configuration.default
): Exported[Codec.AsObject[A]] =
  new Exported(ConfiguredCodec.derived[A])
