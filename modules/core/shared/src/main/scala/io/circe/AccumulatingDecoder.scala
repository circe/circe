package io.circe

import cats.{ ApplicativeError, Semigroup }
import cats.data.NonEmptyChain

@deprecated("Use Decoder", "0.12.0")
object AccumulatingDecoder {
  @deprecated("Use Decoder.AccumulatingResult", "0.12.0")
  final type Result[A] = Decoder.AccumulatingResult[A]

  @deprecated("Use NonEmptyList.catsDataSemigroupForNonEmptyList[DecodingFailure]", "0.12.0")
  final val failureNecInstance: Semigroup[NonEmptyChain[DecodingFailure]] =
    NonEmptyChain.catsDataSemigroupForNonEmptyChain[DecodingFailure]

  @deprecated("Use Decoder.accumulatingResultInstance", "0.12.0")
  final val resultInstance: ApplicativeError[Result, NonEmptyChain[DecodingFailure]] =
    Decoder.accumulatingResultInstance
}
