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

import cats.kernel.Eq
import cats.kernel.instances.all._
import cats.syntax.eq._
import io.circe.{ Codec, Decoder, Encoder }
import io.circe.testing.CodecTests
import io.circe.tests.CirceMunitSuite
import org.scalacheck.{ Arbitrary, Gen }

object DerivedSuite:
  sealed trait Tree
  case class Branch(l: Tree, r: Tree) extends Tree
  case object Leaf extends Tree

  object Tree:
    val encoder: Encoder.AsObject[Tree] = Encoder.AsObject.derived
    val decoder: Decoder[Tree] = Decoder.derived
    val codec: Codec.AsObject[Tree] = Codec.AsObject.derived

    given Arbitrary[Tree] =
      // maxRemainingDepth limits the depth of the tree to prevent a stack overflow in decoder/encoder at Runtime
      // TODO: Fix the Encoder/Decoder to use something like 'defer' to handle deep nested recursive ADTs
      def genTree(maxRemainingDepth: Long): Gen[Tree] =
        if (maxRemainingDepth <= 0) Gen.const(Leaf)
        else
          Gen.oneOf(
            for {
              // the following line is an awkward trick to add sufficient lazyness to avoid endin up
              // in an endless loop at runtime
              f <- Gen.const(Branch.apply)
              l <- genTree(maxRemainingDepth - 1)
              r <- genTree(maxRemainingDepth - 1)
            } yield f(l, r),
            Gen.const(Leaf)
          )
      Arbitrary(genTree(1000))

    given Eq[Tree] = Eq.fromUniversalEquals

class DerivedSuite extends CirceMunitSuite:
  import DerivedSuite._
  checkAll("Decoder[Tree], Encoder[Tree]", CodecTests[Tree](using Tree.decoder, Tree.encoder).codec)
  checkAll("Codec[Tree]", CodecTests[Tree](using Tree.codec, Tree.codec).codec)
