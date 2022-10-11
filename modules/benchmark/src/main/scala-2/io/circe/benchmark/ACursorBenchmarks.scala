package io.circe.benchmark

import io.circe._
import io.circe.syntax._
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

/**
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmark/jmh:run -i 10 -wi 10 -f 2 -t 1 io.circe.benchmark.ACursorBenchmarks"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class ACursorBenchmarks {
  val dataLinear = (0 to 100).foldLeft(Json.True) { (acc, _) =>
    Json.obj("field" := acc)
  }
  val cursorLinear = (0 to 100).foldLeft[ACursor](dataLinear.hcursor) { (acc, _) =>
    acc.downField("field")
  }

  val dataLotsOfArrays = (0 to 100).foldLeft(Json.True) { (acc, _) =>
    Json.fromValues {
      (0 to 99).map(_.asJson) :+ acc
    }
  }
  val cursorLotsOfArrays = (0 to 100).foldLeft[ACursor](dataLotsOfArrays.hcursor) { (acc, _) =>
    (0 to 99).foldLeft(acc.downArray) { (acc, _) =>
      acc.right
    }
  }

  @Benchmark
  def linear: String =
    // CursorOp.opsToPath(cursorLinear.history)
    cursorLinear.pathString

  @Benchmark
  def deepArrays: String =
    // CursorOp.opsToPath(cursorLotsOfArrays.history)
    cursorLotsOfArrays.pathString
}
