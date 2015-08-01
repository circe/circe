package io.jfc.async

import com.twitter.concurrent.{ Spool, SpoolSource }
import com.twitter.io.{ Buf, Reader }
import com.twitter.util.Future
import io.jfc.Json
import io.jfc.jawn.JfcSupportParser
import jawn.{ AsyncParser, Facade, Parser }

/**
 * A rough sketch that needs a lot of fleshing out.
 */
class StreamingParser {
  private[this] val parser: AsyncParser[Json] =
    JfcSupportParser.async(mode = AsyncParser.UnwrapArray)
  private[this] val source: SpoolSource[Json] = new SpoolSource[Json]()

  def feedUtf8(s: String): Unit =
    parser.absorb(s)(JfcSupportParser.facade).fold(source.raise, _.foreach(source.offer))

  def feed(buf: Buf): Unit = parser.absorb(
    Buf.ByteBuffer.Owned.extract(buf)
  )(JfcSupportParser.facade).fold(source.raise, _.foreach(source.offer))

  def close(): Unit = source.close()
  def results: Future[Spool[Json]] = source()
}

object StreamingParser {
  // def fromReader(reader: Reader): Spool[Json] = ???
}
