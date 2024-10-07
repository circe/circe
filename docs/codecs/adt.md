ADT (Algebraic Data Types)
=========================

### ADTs encoding and decoding

The most straightforward way to encode / decode ADTs is by using generic derivation for the case classes but explicitly defined instances for the ADT type.

Consider the following ADT:

```scala mdoc:silent
sealed trait Event

case class Foo(i: Int) extends Event
case class Bar(s: String) extends Event
case class Baz(c: Char) extends Event
case class Qux(values: List[String]) extends Event
```

And the encoder / decoder instances:

```scala mdoc:silent
import cats.syntax.functor._
import io.circe.{ Decoder, Encoder }, io.circe.generic.auto._
import io.circe.syntax._

object GenericDerivation {
  implicit val encodeEvent: Encoder[Event] = Encoder.instance {
    case foo: Foo => foo.asJson
    case bar: Bar => bar.asJson
    case baz: Baz => baz.asJson
    case qux: Qux => qux.asJson
  }

  implicit val decodeEvent: Decoder[Event] =
    List[Decoder[Event]](
      Decoder[Foo].widen,
      Decoder[Bar].widen,
      Decoder[Baz].widen,
      Decoder[Qux].widen
    ).reduceLeft(_ or _)
}
```

Note that we have to call `widen` (which is provided by Cats's `Functor` syntax, which we bring into scope with the first import) on the decoders because the `Decoder` type class is not covariant. The invariance of circe's type classes is a matter of [some controversy](https://twitter.com/Gentmen/status/829431567315513344) (`Argonaut` for example has gone from invariant to covariant and back), but it has enough benefits that it's unlikely to change, which means we need workarounds like this occasionally.

It's also worth noting that our explicit `Encoder` and `Decoder` instances will take precedence over the generically-derived instances we would otherwise get from the `io.circe.generic.auto._` import (see slides from Travis Brown's talk [here](http://meta.plasm.us/slides/scalaworld/#1) for some discussion of how this prioritization works).

We can use these instances like this:

```scala mdoc
import GenericDerivation._
import io.circe.parser.decode

decode[Event]("""{ "i": 1000 }""")

(Foo(100): Event).asJson.noSpaces
```

This works, and if you need to be able to specify the order that the ADT constructors are tried, it's currently the best solution. Having to enumerate the constructors like this is obviously not ideal, though, even if we get the case class instances for free.

Finally, this approach has limitations for ADTs that are recursively defined. See the [Recursive ADT page](recursive-adt.md) for more details.

### A more generic solution

We can avoid the fuss of writing out all the cases by using the `circe-shapes` module:

```scala mdoc:silent
// To suppress previously imported implicit codecs.
import GenericDerivation.{ decodeEvent => _, encodeEvent => _ }

object ShapesDerivation {
  import io.circe.shapes._
  import shapeless.{ Coproduct, Generic }

  implicit def encodeAdtNoDiscr[A, Repr <: Coproduct](implicit
    gen: Generic.Aux[A, Repr],
    encodeRepr: Encoder[Repr]
  ): Encoder[A] = encodeRepr.contramap(gen.to)

  implicit def decodeAdtNoDiscr[A, Repr <: Coproduct](implicit
    gen: Generic.Aux[A, Repr],
    decodeRepr: Decoder[Repr]
  ): Decoder[A] = decodeRepr.map(gen.from)

}
```

And then:

```scala mdoc
import ShapesDerivation._
import io.circe.parser.decode, io.circe.syntax._

decode[Event]("""{ "i": 1000 }""")

(Foo(100): Event).asJson.noSpaces
```

This will work for any ADT anywhere that `encodeAdtNoDiscr` and `decodeAdtNoDiscr` are in scope. If we wanted it to be more limited, we could replace the generic `A` with our ADT types in those definitions, or we could make the definitions non-implicit and define implicit instances explicitly for the ADTs we want encoded this way.

The main drawback of this approach (apart from the extra `circe-shapes` dependency) is that the constructors will be tried in alphabetical order, which may not be what we want if we have ambiguous case classes (where the member names and types are the same).

### The future

The `generic-extras` module provides a little more configurability in this respect. We can write the following, for example:

```scala mdoc:silent
import io.circe.generic.extras.auto._
import io.circe.generic.extras.Configuration

implicit val genDevConfig: Configuration =
  Configuration.default.withDiscriminator("what_am_i")
```

And then:

```scala mdoc
import io.circe.parser.decode, io.circe.syntax._

(Foo(100): Event).asJson.noSpaces

decode[Event]("""{ "i": 1000, "what_am_i": "Foo" }""")
```

Instead of a wrapper object in the JSON we have an extra field that indicates the constructor. This isn't the default behavior since it has some weird corner cases (e.g. if one of our case classes had a member named `what_am_i`), but in many cases it's reasonable and it's been supported in `generic-extras` since that module was introduced.

### Notes

This still doesn't get us exactly what we want, but it's closer than the default behavior. It's also been considered to change `withDiscriminator` to take an `Option[String]` instead of a `String`, with `None` indicating that we don't want an extra field indicating the constructor, giving us the same behavior as our `circe-shape`s instances in the previous section, but haven't been implemented so far.
