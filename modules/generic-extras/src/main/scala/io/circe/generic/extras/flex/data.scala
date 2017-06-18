package io.circe
package generic.extras.flex

case class Chain[A, B](pre: A, post: B)

object Chain
{
  implicit def Retrieve_Chain[A, B]
  (implicit ra: Retrieve[A], rb: Retrieve[B])
  : Retrieve[Chain[A, B]] =
    new Retrieve[Chain[A, B]] {
      def apply(a: Chain[A, B])(cursor: ACursor) = {
        val pre = ra(a.pre)(cursor)
        rb(a.post)(pre)
      }
      def innermostKey(a: Chain[A, B]): Option[String] = rb.innermostKey(a.post)
    }
}

case class Path(keys: List[String])
{
  def descend(key: String) = Path(keys :+ key)
}

object Path
{
  implicit val Retrieve_Path: Retrieve[Path] =
    new Retrieve[Path] {
      def apply(a: Path)(cursor: ACursor) = a.keys.foldLeft(cursor: ACursor)((c, b) => c.downField(b))
      def innermostKey(a: Path): Option[String] = a.keys.lastOption
    }
}

case class Manual(f: ACursor => ACursor)

object Manual
{
  implicit val Retrieve_Manual: Retrieve[Manual] =
    new Retrieve[Manual] {
      def apply(a: Manual)(cursor: ACursor) = a.f(cursor)
      def innermostKey(a: Manual): Option[String] = None
    }
}

case class Ap[A, B, C](pre: A, f: B => C)
{
  def retrieve(implicit retr: Retrieve[A]) = retr.apply(pre) _
}

case class Fmap[A, B, C](pre: A, f: B => C)
{
  def retrieve(implicit retr: Retrieve[A]) = retr.apply(pre) _
}

case class Auto[A]()

case class DefaultValue[A, B](pre: A, value: B)

case class Value[A, V](pre: A)

case class Extraction[A, B](data: B)
