package io.circe.async

import com.twitter.concurrent.AsyncQueue
import com.twitter.concurrent.exp.AsyncStream
import com.twitter.io.{ Buf, InputStreamReader, Reader }
import com.twitter.util.Future
import io.circe.Json
import io.circe.jawn.CirceSupportParser
import java.io.File
import jawn.AsyncParser
import scala.annotation.tailrec


/**
 * A rough sketch that needs a lot of fleshing out.
 */
abstract class StreamingParser {
  private[this] val parser: AsyncParser[Json] =
    CirceSupportParser.async(mode = AsyncParser.UnwrapArray)

  def close(): Future[Unit] = Future.Unit
  def read(): Future[Option[Buf]]

  def results: AsyncStream[Json] = AsyncStream.fromFuture(
    read().flatMap {
      case Some(buf) => parser.absorb(
        Buf.ByteBuffer.Owned.extract(buf)
      )(CirceSupportParser.facade).fold(
        pe => Future.exception(pe),
        js => Future.value(AsyncStream.fromSeq(js) ++ results)
      )
      case None => close().map(_ => AsyncStream.empty)
    }.rescue { case t => close().before(Future.exception(t)) }
  ).flatten
}

object StreamingParser {
  def fromReader(reader: Reader): AsyncStream[Json] = {
    val parser = new StreamingParser {
      def read(): Future[Option[Buf]] = reader.read(4096 * 4)
    }

    parser.results
  }

  /*def fromFile(file: File): AsyncStream[Json] = {
    val reader =
    val parser = new StreamingParser {
      def read(): Future[Option[Buf]] = reader.read(4096 * 4).onFailure()
    }

    parser.results

  }*/
}
