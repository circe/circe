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

import scala.util.Failure
import scala.util.Success
import scala.util.Try

private[circe] trait EnumerationDecoders {

  /**
   * {{{
   *   object WeekDay extends Enumeration { ... }
   *   implicit val weekDayDecoder = Decoder.decodeEnumeration(WeekDay)
   * }}}
   *
   * @group Utilities
   */
  final def decodeEnumeration[E <: Enumeration](enumeration: E): Decoder[enumeration.Value] =
    new Decoder[enumeration.Value] {
      final def apply(c: HCursor): Decoder.Result[enumeration.Value] = Decoder.decodeString(c).flatMap { str =>
        Try(enumeration.withName(str)) match {
          case Success(a) => Right(a)
          case Failure(_) =>
            Left(
              DecodingFailure(
                s"Couldn't decode value '$str'. " +
                  s"Allowed values: '${enumeration.values.mkString(",")}'",
                c.history
              )
            )
        }
      }
    }

}
