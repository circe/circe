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

package io.circe

import io.circe.jawn._

/**
 * Provides Jawn parser on both the JVM and JS. Technically we already have
 * one on the JVM, but since this is test only code doing the platforming
 * work seems like bloat here.
 *
 * The reason this is needed is for testing the printing of Json values
 * complying to some order. The default JS parser will apply an order out of
 * the box that deviates slightly from `Order[String]` and thus causes tests
 * to fail. Note, the issue is not and has never been with the printer. The
 * printer has been emitting values in the correct order, it is the parser
 * that has been re-ordering values.
 *
 * @note If it is decided that circe should use Jawn on JS in the future
 *       (https://github.com/circe/circe/issues/1941), then this can and
 *       should be removed.
 */
private[circe] object TestParser {
  val parser: JawnParser =
    new JawnParser
}
