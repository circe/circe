package io.circe.internal

import io.circe.Printer

trait PunctuationMemoizer { this: Printer =>
  def punctuation(depth: Int): Printer.Punctuation = compute(depth)
}
