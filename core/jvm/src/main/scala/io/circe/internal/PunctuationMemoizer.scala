package io.circe.internal

import io.circe.Printer
import java.util.concurrent.CopyOnWriteArrayList

trait PunctuationMemoizer { this: Printer =>
  private[this] final val known = new CopyOnWriteArrayList[Printer.Punctuation]

  def punctuation(depth: Int): Printer.Punctuation =
    if (depth < known.size) known.get(depth) else if (depth == known.size) {
      val res = compute(depth)
      known.add(depth, res)
      res
    } else {
      var i = known.size
      var res: Printer.Punctuation = null

      while (i <= depth) {
        res = compute(i)
        known.add(i, res)
        i += 1
      }

      res
    }
}
