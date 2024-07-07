Recursive ADT (Algebraic Data Types)
====================================

## Recursive ADT encoding and decoding

Certain shapes of data are difficult to write correct `Decoder`/`Encoder` instances for, 
however much of the complexity can be avoided by leveraging the `Defer` typeclass from `cats`.

### Recursive `Decoder` example

It's important to understand the failure modes that are specific to writing a `Decoder` for a 
recursive data structure, because `Defer` only resolves 3 of the 4.

Consider the following ADT:

```scala mdoc
import io.circe.{Json, Decoder, HCursor}
import io.circe.Decoder.{Result, AccumulatingResult}
import io.circe.syntax._
import cats.syntax.all._

sealed trait Tree[A]
case class Branch[A](left: Tree[A], right: Tree[A]) extends Tree[A]
case class Leaf[A](value: A) extends Tree[A]
```

#### First Attempt: `StackOverflowError` during `Decoder` creation
And these encoder / decoder instances:

```scala mdoc:nest
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
```scala mdoc:nest
val decoder = 
  try implicitly[Decoder[Tree[Int]]] 
  catch {
    case _: StackOverflowError => 
      Decoder.failedWithMessage[Tree[Int]]("Lookup caused StackOverflowError")
  }

Json.obj("v" := 1).as[Tree[Int]](decoder)
```

This implementation looks quite reasonable, however it will result in a `StackOverflowError` at runtime.
This happens because `Tree` is generic, and it's `Decoder` instances must be generic `def`s, producing and
endless loop of calls to `treeDecoder` and `branchDecoder`.

#### Second Attempt: Diverging Implicits error

The `StackOverflowError` can be converted to a compilation error by adjusting the implicit parameters to be 
more granular. This diverging implicits error is more clear about the source of the issue.

```scala mdoc:nest
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
```scala mdoc:fail
implicitly[Decoder[Tree[Int]]]
```

#### Third Attempt: Undesirable creation of `Decoder` instances

Switching to lazily creating the instances needed is a way to get past the diverging implicits issue, however the
cost of this approach is the creation of a new instance of `Decoder` each time recursion occurs.

```scala mdoc:nest
var instanceCounter = 0

implicit def branchDecoder[A: Decoder]: Decoder[Branch[A]] = {
  instanceCounter += 1
  Decoder.instance { c =>
    (c.downField("l").as[Tree[A]], c.downField("r").as[Tree[A]]).mapN(Branch.apply)
  }
}

implicit def leafDecoder[A: Decoder]: Decoder[Leaf[A]] = {
  instanceCounter += 1
  Decoder[A].at("v").map(Leaf(_))
}

implicit def treeDecoder[A: Decoder]: Decoder[Tree[A]] = {
  instanceCounter += 1
  Decoder.instance { c =>
    c.as[Branch[A]].orElse(c.as[Leaf[A]])
  }
}
```
```scala mdoc:nest
Json.obj(
  "l" -> Json.obj("v" := 1),
  "r" -> Json.obj(
    "l" -> Json.obj("v" := 2),
    "r" -> Json.obj("v" := 3)
  )
).as[Tree[Int]]

instanceCounter
```

#### Fourth Attempt: A workable solution

The solution to the previous issues is to generate the instances once, and cache them.

```scala mdoc:nest
var instanceCounter = 0

implicit def branchDecoder[A](implicit DTA: Decoder[Tree[A]]): Decoder[Branch[A]] = {
  instanceCounter += 1
  Decoder.forProduct2[Branch[A], Tree[A], Tree[A]]("l", "r")(Branch.apply)
}

implicit def leafDecoder[A: Decoder]: Decoder[Leaf[A]] = {
  instanceCounter += 1
  Decoder[A].at("v").map(Leaf(_))
}

implicit def treeDecoder[A: Decoder]: Decoder[Tree[A]] = {
  instanceCounter += 1
  new Decoder[Tree[A]] {
    private implicit val self: Decoder[Tree[A]] = this
    private val delegate =
      List[Decoder[Tree[A]]](
        Decoder[Branch[A]].widen,
        Decoder[Leaf[A]].widen
      ).reduce(_ or _)
      
    override def apply(c: HCursor): Result[Tree[A]] = delegate(c)
  }
}
```
```scala mdoc:nest
Json.obj(
  "l" -> Json.obj("v" := 1),
  "r" -> Json.obj(
    "l" -> Json.obj("v" := 2),
    "r" -> Json.obj("v" := 3)
  )
).as[Tree[Int]]

instanceCounter
```

Note that, because `delegate` is a fixed instance, it is not necessary to explicitly cache
`branchDecoder` or `leafDecoder`.

This implementation solves _most_ of the issues with a recursive decoder, at the cost of 
being annoying to write, and easy to break by omitting `private implicit val self: Decoder[Tree[A]] = this`,
which will cause this decoder to produce a `StackOverflowError` at runtime, but the compiler 
won't complain about removing it.

#### Fifth Attempt: A more elegant solution

The `Defer` typeclass allows us to write `Decoder`s and `Encoders` that have access to their eventual 
instance, enabling us to write an equivalent instances in a more natural and less fragile fashion. 
This can be done directly, using `Defer[Decoder].fix`, or the `recursive` helper provided on the `Decoder` 
and `Encoder` companion objects.

The `Decoder` instances for `Tree` using `Decoder.recursive` would look like this: 
```scala mdoc:nest
var instanceCounter = 0

implicit def branchDecoder[A](implicit DTA: Decoder[Tree[A]]): Decoder[Branch[A]] = {
  instanceCounter += 1
  Decoder.forProduct2[Branch[A], Tree[A], Tree[A]]("l", "r")(Branch.apply)
}

implicit def leafDecoder[A: Decoder]: Decoder[Leaf[A]] = {
  instanceCounter += 1
  Decoder[A].at("v").map(Leaf(_))
}

implicit def treeDecoder[A: Decoder]: Decoder[Tree[A]] = {
  instanceCounter += 1
  Decoder.recursive { implicit recurse =>
    List[Decoder[Tree[A]]](
      Decoder[Branch[A]].widen,
      Decoder[Leaf[A]].widen
    ).reduce(_ or _)
  }
}
```
```scala mdoc:nest
Json.obj(
  "l" -> Json.obj("v" := 1),
  "r" -> Json.obj(
    "l" -> Json.obj("v" := 2),
    "r" -> Json.obj("v" := 3)
  )
).as[Tree[Int]]

instanceCounter
```

Note that, in the same manner that caching `delegate` implicitly caches the branch and leaf decoders, 
because `treeDecoder` provides a fixed instance, it is not necessary to use `Decoder.recursive` to write
`branchDecoder` or `leafDecoder`

### Limitations

While both the fourth and fifth attempts avoid the issues of the previous attempts, they are not
perfect. Because the recursive calls are not tail recursive, they are not fully stack safe. This
is generally not an issue for data structures that have a depth of at most `log(size)`, linear 
structures (like a linked list) could run into trouble.

```scala mdoc:nest
var instanceCounter = 0

implicit def branchDecoder[A](implicit DTA: Decoder[Tree[A]]): Decoder[Branch[A]] = {
  instanceCounter += 1
  Decoder.forProduct2[Branch[A], Tree[A], Tree[A]]("l", "r")(Branch.apply)
}

implicit def leafDecoder[A: Decoder]: Decoder[Leaf[A]] = {
  instanceCounter += 1
  Decoder[A].at("v").map(Leaf(_))
}

implicit def treeDecoder[A: Decoder]: Decoder[Tree[A]] = {
  instanceCounter += 1
  Decoder.recursive { implicit recurse =>
    List[Decoder[Tree[A]]](
      Decoder[Branch[A]].widen,
      Decoder[Leaf[A]].widen
    ).reduce(_ or _)
  }
}
```
```scala mdoc:nest
val decoder = Decoder[Tree[Int]]
try 
  List
    .range(0, 10000)
    .map(i => Json.obj("v" := i))
    .reduceLeft { (a, b) =>
      Json.obj("l" -> a, "r" -> b)
    }
    .as[Tree[Int]](decoder)
catch {
  case _: StackOverflowError => Leaf(-1).asRight
}

instanceCounter
```
Note that despite the successful creation of a `Decoder[Tree[Int]]` instance, and the number of `Decoder` 
instances remaining constant at 3, the size of the unbalanced `Tree[Int]` represented in the JSON still
caused a `StackOverflowError`.