package io.circe
package generic.extras.flex

import scala.reflect.macros.whitebox
import scala.language.implicitConversions

import cats.Functor
import cats.implicits._

import shapeless.{::, Witness, Generic, Poly2, HList, HNil}
import shapeless.labelled.{FieldType, field}
import shapeless.poly.Case2

trait Retrieve[A]
{
  def apply(a: A)(cursor: ACursor): ACursor
  def innermostKey(a: A): Option[String]
  def ap[B, C](a: A)(f: B => C) = Ap(a, f)
  def map[B, C](a: A)(f: B => C) = Fmap(a, f)
  def mkValue(a: A) = Value(a)
}

class RetrieveOps[A](self: A)
(implicit retrieve: Retrieve[A])
{
  def ap[B, C](f: B => C) = retrieve.ap(self)(f)
  def map[B, C](f: B => C) = retrieve.map(self)(f)
  def mkValue = retrieve.mkValue(self)
}

class RetrieveFieldOps[K, A](self: FieldType[K, A])
(implicit retrieve: Retrieve[A])
{
  def ap[B, C](f: B => C) = field[K](retrieve.ap(self)(f))

  def >>[B, C](f: B => C) = ap(f)

  def map[B, C](f: B => C) = field[K](retrieve.map(self)(f))

  def >>>[B, C](f: B => C) = map(f)

  def value[B] = field[K](Value[A, B](self: A))

  def \(key: String): FieldType[K, Chain[A, Path]] = field[K](Chain(self, Path(List(key))))

  def \(key: Symbol): FieldType[K, Chain[A, Path]] = field[K](Chain(self, Path(List(key.name))))

  def fetch(f: ACursor => ACursor): FieldType[K, Chain[A, Manual]] = field[K](Chain(self, Manual(f)))
}

trait Extract[A]
{
  type Out

  def apply[K <: Symbol: Witness.Aux](a: A)(cursor: HCursor): Decoder.Result[Out]
}

object Extract
{
  type Aux[A, Out0] = Extract[A] { type Out = Out0 }

  def apply[A, B](implicit instance: Extract.Aux[A, B]) = instance

  implicit def Extract_Retrieve[A, B: Decoder](implicit R: Retrieve[A]): Aux[A, B] =
    new Extract[A] {
      type Out = B
      def apply[K <: Symbol: Witness.Aux](a: A)(cursor: HCursor): Decoder.Result[B] = {
        R(a)(cursor).as[B]
      }
    }

  implicit def Extract_Function1[A: Decoder, B]: Aux[A => B, B] =
    new Extract[A => B] {
      type Out = B
      def apply[K <: Symbol](f: A => B)(cursor: HCursor)(implicit key: Witness.Aux[K]): Decoder.Result[B] = {
        val inner = Extract[Ap[Path, A, B], B]
        inner[K](Path(List(key.value.name)).ap(f))(cursor)
      }
    }

  implicit def Extract_Ap[A, B: Decoder, C]
  (implicit inner: Extract.Aux[A, B])
  : Aux[Ap[A, B, C], C]
  = new Extract[Ap[A, B, C]] {
      type Out = C
      def apply[K <: Symbol: Witness.Aux](a: Ap[A, B, C])(cursor: HCursor): Decoder.Result[C] = {
        inner(a.pre)(cursor).map(a.f)
      }
    }

  implicit def Extract_Fmap[F[_], A, B, C]
  (implicit dec: Decoder[F[B]], inner: Extract.Aux[A, F[B]], functor: Functor[F])
  : Aux[Fmap[A, B, C], F[C]]
  = new Extract[Fmap[A, B, C]] {
      type Out = F[C]
      def apply[K <: Symbol: Witness.Aux](a: Fmap[A, B, C])(cursor: HCursor): Decoder.Result[F[C]] = {
        inner(a.pre)(cursor).map(functor.map(_)(a.f))
      }
    }

  implicit def Extract_Value[A, B, C]
  (implicit inner: Extract.Aux[A, B], gen: Generic.Aux[C, B :: HNil])
  : Aux[Value[A, B], C] =
    new Extract[Value[A, B]] {
      type Out = C
      def apply[K <: Symbol: Witness.Aux](a: Value[A, B])(cursor: HCursor): Decoder.Result[C] = {
        inner(a.pre)(cursor).map(_ :: HNil).map(Generic[C].from)
      }
    }

  implicit def Extract_Manual[A: Decoder]: Aux[Manual, A] =
    new Extract[Manual] {
      type Out = A
      def apply[K <: Symbol: Witness.Aux](a: Manual)(cursor: HCursor): Decoder.Result[A] = {
        a.f(cursor).as[A]
      }
  }

  implicit def Extract_Auto[A: Decoder]: Aux[Auto[A], A] =
    new Extract[Auto[A]] {
      type Out = A
      def apply[K <: Symbol](a: Auto[A])(cursor: HCursor)(implicit key: Witness.Aux[K]): Decoder.Result[A] = {
        cursor.downField(key.value.name).as[A]
      }
    }

  implicit def Extract_DefaultValue[A, B]
  (implicit inner: Extract.Aux[A, B])
  : Aux[DefaultValue[A, B], B] =
    new Extract[DefaultValue[A, B]] {
      type Out = B
      def apply[K <: Symbol: Witness.Aux](a: DefaultValue[A, B])(cursor: HCursor): Decoder.Result[B] = {
        inner(a.pre)(cursor).orElse(Right(a.value))
      }
    }
}

class ExtractFieldOps[K, A: Extract](self: FieldType[K, A])
{
  def value[B] = field[K](Value[A, B](self: A))

  def ap[B, C](f: B => C) = field[K](Ap(self: A, f))

  def >>[B, C](f: B => C) = ap(f)

  def map[B, C](f: B => C) = field[K](Fmap(self: A, f))

  def >>>[B, C](f: B => C) = map(f)
}

class FmapFieldOps[K, A, B, C](self: FieldType[K, Fmap[A, B, C]])
{
  def ap[F[_], D](f: F[C] => F[D]) = field[K](Ap(self: Fmap[A, B, C], f))

  def >>[F[_], D](f: F[C] => F[D]) = ap(f)

  def map[D](f: C => D) = field[K](Fmap(self: Fmap[A, B, C], f))

  def >>>[D](f: C => D) = map(f)
}

trait SymbolOps
{
  type K <: Symbol

  val self: Symbol

  def \\(key: String): FieldType[K, Path] = field[K](Path(List(key)))

  def \\(key: Symbol): FieldType[K, Path] = field[K](Path(List(key.name)))

  def \(key: String): FieldType[K, Path] = field[K](Path(List(self.name, key)))

  def \(key: Symbol): FieldType[K, Path] = field[K](Path(List(self.name, key.name)))

  def value[A](implicit key: Witness.Aux[K]) = field[K](Value[Path, A](Path(List(key.value.name))))

  def fetch(f: ACursor => ACursor): FieldType[K, Manual] = field[K](Manual(f))

  def ap[B, C](f: B => C)(implicit key: Witness.Aux[K]) = field[K](Ap(Path(List(key.value.name)), f))

  def >>[B, C](f: B => C)(implicit key: Witness.Aux[K]) = ap(f)

  def map[B, C](f: B => C)(implicit key: Witness.Aux[K]) = field[K](Fmap(Path(List(key.value.name)), f))

  def >>>[B, C](f: B => C)(implicit key: Witness.Aux[K]) = map(f)

  def in(path: Symbol*)(implicit key: Witness.Aux[K]) = field[K](Path(path.toList.map(_.name)).descend(key.value.name))
}

class Dsl(val c: whitebox.Context)
{
  val sl = new shapeless.SingletonTypeMacros(c)

  import c.universe.Tree
  import sl.c.universe.{Tree => SLTree, _}

  val ops = symbolOf[SymbolOps]

  def makeOps(s: Tree): Tree =
    sl.extractResult(s.asInstanceOf[SLTree]) {
      (tpe, value) =>
        q"""
        new $ops {
          type K = $tpe
          val self: Symbol = $value
        }
        """
    }.asInstanceOf[Tree]
}

class PathDsl[K <: Symbol: Witness.Aux](p: FieldType[K, Path])
{
  def \(key: String): FieldType[K, Path] = field[K](p.descend(key))

  def \(key: Symbol): FieldType[K, Path] = field[K](p.descend(key.name))
}

trait MapWith[HF <: Poly2, L <: HList, In]
{
  type Out <: HList

  def apply(l: L, in: In): Out
}

object MapWith
{
  def apply[HF <: Poly2, L <: HList, In](implicit mw: MapWith[HF, L, In]): Aux[HF, L, In, mw.Out] = mw

  type Aux[HF <: Poly2, L <: HList, A, Out0] =
    MapWith[HF, L, A] { type Out = Out0 }

  implicit def hnilMapWith[HF <: Poly2, T <: HNil, In]: Aux[HF, T, In, HNil] =
    new MapWith[HF, T, In] {
      type Out = HNil
      def apply(l: T, in: In): Out = HNil
    }

  implicit def hlistMapWith[HF <: Poly2, H, T <: HList, In, OutH, OutT <: HList]
  (implicit
    f: Case2.Aux[HF, H, In, OutH],
    mt: MapWith.Aux[HF, T, In, OutT]
    )
  : Aux[HF, H :: T, In, OutH :: OutT] =
    new MapWith[HF, H :: T, In] {
      type Out = OutH :: OutT
      def apply(l: H :: T, in: In): Out = f(l.head, in) :: mt(l.tail, in)
    }
}

final class MapWithOps[L <: HList](self: L)
{
  def mapWith[A, F <: Poly2](f: F)(a: A)(implicit mw: MapWith[F, L, A]): mw.Out = mw(self, a)
}

object syntax
{
  object mapWith
  {
    implicit def ToMapWithOps[L <: HList](l: L): MapWithOps[L] = new MapWithOps(l)
  }
}
