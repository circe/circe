---
layout: docs
title: "Recursive ADT (Algebraic Data Types)
---

### Recursive ADT encoding and decoding

Certain shapes of data are difficult to write correct `Decoder`/`Encoder` instances for, 
however much of the complexity can be avoided by leveraging the `Defer` typeclass from `cats`.

Consider the following ADT:

```scala mdoc:silent
sealed trait Tree[A]
case class Branch[A](left: Tree[A], right: Tree[A]) extends Tree[A]
case class Leaf[A](value: A) extends Tree[A]
```

And these encoder / decoder instances:

```scala mdoc:silent
import io.circe.{Json, Decoder}
import io.circe.syntax._

implicit def branchDecoder[A: Decoder]: Decoder[Branch[A]] =
  Decoder.forProduct2[Branch[A], Tree[A], Tree[A]]("l", "r")(Branch.apply)

implicit def leafDecoder[A: Decoder]: Decoder[Leaf[A]] =
  Decoder[A].at("v").map(Leaf(_))

implicit def treeDecoder[A: Decoder]: Decoder[Tree[A]] =
  List[Decoder[Tree[A]]](
    Decoder[Branch[A]].widen,
    Decoder[Leaf[A]].widen
  ).reduceLeft(_ or _)
```

This implementation looks quite reasonable, however it will result in a `StackOverflowError` at runtime.
This happens because `Tree` is generic, and it's `Decoder` instances must be generic `def`s, producing and
endless loop of calls to `treeDecoder` and `branchDecoder`.

Adjusting the implicit parameters to be more granular produces a diverging implicits error that is more
clear about the source of the issue:

```scala mdoc:silent
import io.circe.{Json, Decoder}
import io.circe.syntax._

implicit def branchDecoder[A](implicit TD: Decoder[Tree[A]]): Decoder[Branch[A]] =
  Decoder.forProduct2[Branch[A], Tree[A], Tree[A]]("l", "r")(
    Branch.apply
  )

implicit def leafDecoder[A: Decoder]: Decoder[Leaf[A]] =
  Decoder[A].at("v").map(Leaf(_))

implicit def treeDecoder[A](implicit BD: Decoder[Branch[A]], LD: Decoder[Leaf[A]]): Decoder[Tree[A]] =
  List[Decoder[Tree[A]]](
    Decoder[Branch[A]].widen,
    Decoder[Leaf[A]].widen
  ).reduce(_ or _)
```

### A working solution

The `Defer` typeclass allows us to write `Decoder`s and `Encoders` that have access to their eventual 
instance, enabling us to write these instances in a more natural fashion. This can be done directly, 
using `Defer[Decoder].fix`, or the `recursive` helper provided on the `Decoder` and `Encoder` companion
objects.

The `Decoder` instances for `Tree` would look like this: 
```scala mdoc:silent
import io.circe.{Json, Decoder}
import io.circe.syntax._

implicit def branchDecoder[A](implicit DTA: Decoder[Tree[A]]): Decoder[Branch[A]] =
  Decoder.forProduct2[Branch[A], Tree[A], Tree[A]]("l", "r")(Branch.apply)

implicit def leafDecoder[A: Decoder]: Decoder[Leaf[A]] =
  Decoder[A].at("v").map(Leaf(_))

implicit def treeDecoder[A: Decoder]: Decoder[Tree[A]] =
  Decoder.recursive { implicit recurse =>
    List[Decoder[Tree[A]]](
      Decoder[Branch[A]].widen,
      Decoder[Leaf[A]].widen
    ).reduce(_ combine _)
  }
```

Note that, because `treeDecoder` provides a fixed instance, it is not necessary to also wrap 
`branchDecoder` or `leafDecoder`.

### Notes

It's possible to manually create an equivalent `Decoder` to what is produced by using `Defer`,
however it's generally not worth the trouble.

Consider these decoders:
```scala mdoc:silent
import io.circe.{Json, Decoder}
import io.circe.syntax._

implicit def branchDecoder[A](implicit DTA: Decoder[Tree[A]]): Decoder[Branch[A]] =
  Decoder.forProduct2[Branch[A], Tree[A], Tree[A]]("left", "right")(Branch.apply)

implicit def leafDecoder[A: Decoder]: Decoder[Leaf[A]] =
  Decoder[A].at("value").map(Leaf(_))

implicit def treeDecoder[A: Decoder]: Decoder[Tree[A]] =
  new Decoder[Tree[A]] {
    private implicit val self: Decoder[Tree[A]] = this
    private val delegate =
      List[Decoder[Tree[A]]](
        Decoder[Branch[A]].widen,
        Decoder[Leaf[A]].widen
      ).reduce(_ combine _)
      
    override def apply(c: HCursor): Result[Tree[A]] = delegate(c)
    override def decodeAccumulating(c: HCursor): AccumulatingResult[Tree[A]] = delegate.decodeAccumulating(c)
  }
```

Accidentally commenting out `private implicit val self: Decoder[Tree[A]] = this` will cause this 
decoder to produce a `StackOverflowError` at runtime, but the compiler won't complain about removing it.