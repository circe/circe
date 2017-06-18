package io.circe
package generic
package extras
package flex

import scala.language.experimental.macros
import scala.language.implicitConversions

import shapeless.Witness
import shapeless.labelled.FieldType

import cats.instances.EitherInstances

trait FlexFunctions0
{
  implicit def extractFieldOps[K, A: Extract](a: FieldType[K, A]): ExtractFieldOps[K, A] =
    new ExtractFieldOps(a)
}

trait FlexFunctions
extends FlexFunctions0
{
  def deriveH[A]: DeriveDecoder[A] = new DeriveDecoder[A]

  def derive[A](extra: Any*): Decoder[A] = macro DeriveMacro.derive[A]

  implicit def dsl(s: Symbol): SymbolOps = macro Dsl.makeOps

  implicit def pathDsl[K <: Symbol: Witness.Aux](a: FieldType[K, Path]): PathDsl[K] = new PathDsl(a)

  implicit def retrieveOps[A: Retrieve](a: A): RetrieveOps[A] = new RetrieveOps(a)

  implicit def retrieveOpsField[K, A: Retrieve](a: FieldType[K, A]): RetrieveOps[A] = new RetrieveOps(a: A)

  implicit def retrieveFieldOps[K, A: Retrieve](a: FieldType[K, A]): RetrieveFieldOps[K, A] =
    new RetrieveFieldOps(a)

  implicit def fmapFieldOps[K, A, B, C](a: FieldType[K, Fmap[A, B, C]]): FmapFieldOps[K, A, B, C] =
    new FmapFieldOps(a)
}

object `package`
extends FlexFunctions
{
  object all
  extends FlexFunctions
  with EitherInstances
  with AutoDerivation
}
