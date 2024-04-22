/*
 * Copyright 2024 circe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.circe

import io.circe.tests.CirceMunitSuite
import org.scalacheck.Prop
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.concurrent.duration._

class PrinterWriterReuseSuite extends CirceMunitSuite {
  implicit val ec: ExecutionContext = ExecutionContext.global

  override protected val scalaCheckTestParameters =
    super.scalaCheckTestParameters.withMaxSize(100)

  property("Printer#reuseWriters should not change the behavior of print")(reuseWritersProp)

  val reuseWritersProp: Prop = Prop.forAll { (values: List[Json]) =>
    val default = Printer.spaces4
    val reusing = default.copy(reuseWriters = true)
    val expected = values.map(default.print)

    val merged = Future.traverse(values)(value => Future(reusing.print(value)))
    assertEquals(Await.result(merged, 1.second), expected)
  }
}
