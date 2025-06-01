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

package io.circe.internal.fix

import scalafix.v1._
import scala.meta._
import scala.annotation.tailrec
import scalafix.lint.LintSeverity

/**
 * A Scalafix rule which rewrites MUnit tests or ScalaCheck property tests to
 * use assertions/properties which have a notion of actual vs. expected
 * result, e.g. `assertEquals` or `?=`. Using these rather than
 * `assert(actual == expected)` gives much better failure messages.
 *
 * This is a simple syntactic rule and it is not intended to be used outside
 * of Circe. It doesn't handle everything, and will likely require a very
 * small amount of manual fixing after run, e.g. adding a missing import or
 * adding `&&` between new properties.
 *
 * @note This can not be a singleton object, because of how Scalafix uses JVM
 *       service discovery.
 */
final class AssertRewrite extends SyntacticRule("AssertRewrite") {
  import AssertRewrite._

  /**
   * Check if a tree is an application of a symbol named "forAll" to some
   * arguments.
   */
  private def isForAll(tree: Tree): Boolean =
    isApplyWithName(tree, "forAll")

  /**
   * Check if a tree is an application of a symbol named "test" to some
   * arguments.
   */
  private def isTest(tree: Tree): Boolean =
    isApplyWithName(tree, "test")

  /**
   * Check if a tree is an application of a symbol by a given name to
   * arguments.
   */
  private def isApplyWithName(tree: Tree, name: String): Boolean =
    applyWithName[Boolean](tree, name, _ => true, false)

  /**
   * If a symbol with a given name is applied to some arguments, then run `f`
   * on that tree, otherwise yield `default`.
   */
  private def applyWithName[A](tree: Tree, name: String, f: Tree => A, default: A): A =
    tree match {
      case t @ Term.Apply.Initial(Term.Apply.Initial(Term.Name(n), _), _) if n == name => f(t)
      case t @ Term.Block(Term.Apply.Initial(Term.Name(n), _) :: _) if n == name       => f(t)
      case t @ Defn.Val(_, _, _, Term.Apply.Initial(Term.Name(n), _)) if n == name     => f(t)
      case _                                                                           => default
    }

  /**
   * Convert an `assert` expression to an `assertEquals` expression.
   */
  private def assertToAssertEquals(tree: Tree): Patch =
    tree match {
      case t @ Term.Apply.Initial(
            Term.Name("assert"),
            List(Term.ApplyInfix.Initial(actual, Term.Name(op), List(), List(expected)))
          ) if op == "==" || op == "===" =>
        Patch.replaceTree(t, s"assertEquals($actual, $expected)")
      case _ =>
        Patch.empty
    }

  /**
   * Convert an `assert` expression to an `actual ?= expected` expression.
   */
  private def assertToPropEquals(tree: Tree): Patch =
    tree match {
      case t @ Term.Apply.Initial(
            Term.Name("assert"),
            List(Term.ApplyInfix.Initial(actual, Term.Name(op), List(), List(expected)))
          ) if op == "==" || op == "===" =>
        Patch.replaceTree(t, s"$actual ?= $expected")
      case t @ Term.Apply.Initial(Term.Name("assertEquals"), List(actual, expected)) =>
        Patch.replaceTree(t, s"$actual ?= $expected")
      case _ =>
        Patch.empty
    }

  /** Convert a test to a property check, if that test is using forAll. */
  private def testToProperty(tree: Tree): Patch =
    tree match {
      case Term.Apply.Initial(
            Term.Apply.Initial(t @ Term.Name("test"), _),
            Term.Block(Term.Apply.Initial(Term.Name("forAll"), _) :: _) :: _
          ) =>
        Patch.replaceTree(t, "property")
      case _ =>
        Patch.empty
    }

  override def fix(implicit doc: SyntacticDocument): Patch = {

    def updateConfigs(loc: Tree, cur: List[Config], state: State): List[Config] =
      loc.children.map(child => Config(state, child)) ++ cur

    @tailrec
    def loop(configs: List[Config], acc: Set[Patch]): Set[Patch] = {
      configs match {
        case Nil     => acc
        case x :: xs =>
          val update: State => List[Config] =
            s => updateConfigs(x.loc, xs, s)
          x match {
            case Config(state, loc) if isTest(loc) =>
              if (state.inTest) {
                loop(
                  update(state),
                  acc + Patch.lint(
                    Diagnostic("assertRewrite", "Unexpected nested test.", loc.pos, "", LintSeverity.Error)
                  )
                )
              } else {
                update(state.copy(inTest = true)) match {
                  case configs =>
                    if (state.inForAll) {
                      loop(
                        configs,
                        acc + Patch.lint(
                          Diagnostic(
                            "assertRewrite",
                            "test invocation nested inside forAll invocation.",
                            loc.pos,
                            "",
                            LintSeverity.Error
                          )
                        )
                      )
                    } else {
                      loop(configs, acc + testToProperty(loc))
                    }
                }
              }
            case Config(state, loc) if isForAll(loc) =>
              update(state.copy(inForAll = true)) match {
                case configs => loop(configs, acc)
              }
            case Config(state, loc) =>
              update(state) match {
                case configs =>
                  if (state.inTest && state.inForAll) {
                    loop(configs, acc)
                  } else if (state.inTest) {
                    loop(configs, acc + assertToAssertEquals(loc))
                  } else if (state.inForAll) {
                    loop(configs, acc + assertToPropEquals(loc))
                  } else {
                    loop(configs, acc)
                  }
              }
          }
      }
    }

    Patch.fromIterable(loop(List(Config(State.empty, doc.tree)), Set.empty))
  }
}

object AssertRewrite {
  val instance: AssertRewrite = new AssertRewrite

  private final case class State(inForAll: Boolean, inTest: Boolean)

  private object State {
    val empty: State = State(false, false)
  }

  private final case class Config(state: State, loc: Tree)
}
