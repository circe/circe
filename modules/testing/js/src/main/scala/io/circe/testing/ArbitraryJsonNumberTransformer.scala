/*
 * Copyright 2023 circe
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

package io.circe.testing

import io.circe.JsonNumber
import scala.scalajs.js.JSON
import scala.util.Try

/**
 * We only want to generate arbitrary [[JsonNumber]] values that Scala.js can
 * parse.
 */
private[testing] trait ArbitraryJsonNumberTransformer {
  def transformJsonNumber(n: JsonNumber): JsonNumber =
    Try(JSON.parse(n.toString): Any).toOption.filter {
      case x: Double => !x.isInfinite && n.toBigDecimal.exists(_ == BigDecimal(x))
      case _         => true
    }.fold(JsonNumber.fromIntegralStringUnsafe("0"))(_ => n)
}
