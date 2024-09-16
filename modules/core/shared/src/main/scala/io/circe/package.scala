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

package io

// scalastyle:off
/**
 * ==Overview==
 *
 *  This is the API documentation for [[http://circe.io Circe]], a JSON library for
 *  Scala and Scala.js.
 *
 *  The library is divided into a number of modules that either provide core
 *  functionality, support additional features via third-party dependencies, or
 *  facilitate integration with other libraries or frameworks.
 *
 *   - [[io.circe.numbers numbers]]: core facilities for representing and parsing
 *     JSON numbers.
 *   - [[io.circe core]]: our JSON abstract syntax tree, our zipper-like cursor
 *     types, and our encoding and decoding type classes (and instances).
 *   - [[https://github.com/circe/circe/blob/master/modules/parser/jvm/src/main/scala/io/circe/parser/package.scala parser]]: JSON parsing support for both the Java Virtual
 *     Machine (using Jawn) and Scala.js (using the native JavaScript JSON parser).
 *   - [[io.circe.testing testing]]: ScalaCheck `Arbitrary` instances for circe's
 *     JSON AST and other types, as well as other useful testing tools.
 *   - [[io.circe.literal literal]]: JSON string interpolation and encoder and
 *     decoder instances for literal types.
 *   - [[io.circe.generic generic]]: Shapeless-powered
 *     [[https://circe.github.io/circe/codec.html generic derivation]] for case
 *     classes and sealed trait hierarchies.
 *   - [[https://github.com/circe/circe-generic-extras generic-extras]]: additional experimental generic
 *     derivation functionality (including some configurability).
 *   - [[io.circe.pointer pointer]]: A JSON Pointer implementation
 *   - [[io.circe.pointer.literal pointer-literal]]: JSON Pointer string interpolation
 *   - [[io.circe.shapes shapes]]: encoders and decoders for
 *     [[https://github.com/milessabin/shapeless Shapeless]] hlists, coproducts, records, and sized collections.
 *   - [[https://github.com/circe/circe/blob/master/modules/scodec/shared/src/main/scala/io/circe/scodec/package.scala scodec]]: encoders and decoders for
 *     [[https://github.com/scodec/scodec-bits Scodec]]'s `BitVector` and `ByteVector`.
 *   - [[https://github.com/circe/circe/blob/master/modules/refined/shared/src/main/scala/io/circe/refined/package.scala refined]]: encoders and decoders for [[https://github.com/fthomas/refined refined]] types.
 *   - [[https://github.com/circe/circe-spray/blob/master/core/src/main/scala/io/circe/spray/JsonSupport.scala spray]]: Spray marshaller conversions for Circe's type classes.
 *
 *  Please refer to the [[https://circe.github.io/circe/ documentation]] for a more
 *  detailed introduction to the library.
 */
// scalastyle:on
package object circe {
    private[circe] def void[A](a: => A): Unit = {
        val _ = a
        ()
    }
}
