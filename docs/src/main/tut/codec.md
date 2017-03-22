---
layout: docs
title:  "Encoding and decoding"
position: 3
---

# Encoding and decoding

circe uses `Encoder` and `Decoder` type classes for encoding and decoding. An `Encoder[A]` instance
provides a function that will convert any `A` to a `Json`, and a `Decoder[A]` takes a `Json` value
to either an exception or an `A`. circe provides implicit instances of these type classes for many
types from the Scala standard library, including `Int`, `String`, and [others][encoder]. It also
provides instances for `List[A]`, `Option[A]`, and other generic types, but only if `A` has an
`Encoder` instance.

Encoding data to `Json` can be done using the `.asJson` syntax:

```tut:book
import io.circe.syntax._

val intsJson = List(1, 2, 3).asJson
```

Use the `.as` syntax for decoding data from `Json`:

```tut:book
intsJson.as[List[Int]]
```

The `decode` function from the included [parser] module can be used to directly decode
a JSON `String`:

```tut:book
import io.circe.parser.decode

decode[List[Int]]("[1, 2, 3]")
```

## Semi-automatic derivation

Sometimes it's convenient to have an `Encoder` or `Decoder` defined in your code, and semi-automatic
derivation can help. You'd write:

```tut:silent
import io.circe._, io.circe.generic.semiauto._

case class Foo(a: Int, b: String, c: Boolean)

implicit val fooDecoder: Decoder[Foo] = deriveDecoder[Foo]
implicit val fooEncoder: Encoder[Foo] = deriveEncoder[Foo]
```

Or simply:

```tut:silent
implicit val fooDecoder: Decoder[Foo] = deriveDecoder
implicit val fooEncoder: Encoder[Foo] = deriveEncoder
```

### @JsonCodec

The circe-generic project includes a `@JsonCodec` annotation that simplifies the
use of semi-automatic generic derivation:

```tut:book
import io.circe.generic.JsonCodec, io.circe.syntax._

@JsonCodec case class Bar(i: Int, s: String)

Bar(13, "Qux").asJson
```

This works with both case classes and sealed trait hierarchies.

NOTE: You will need the [Macro Paradise](http://docs.scala-lang.org/overviews/macros/paradise) plugin to use annotation macros like `@JsonCodec`

### forProductN helper methods

It's also possible to construct encoders and decoders for case class-like types
in a relatively boilerplate-free way without generic derivation:

```tut:silent
case class User(id: Long, firstName: String, lastName: String)

object UserCodec {
  implicit val decodeUser: Decoder[User] =
    Decoder.forProduct3("id", "first_name", "last_name")(User.apply)

  implicit val encodeUser: Encoder[User] =
    Encoder.forProduct3("id", "first_name", "last_name")(u =>
      (u.id, u.firstName, u.lastName)
    )
}
```

It's not as clean or as maintainable as generic derivation, but it's less magical, it requires nothing
but circe-core, and if you need a custom name mapping it's currently the best solution
(although 0.6.0 introduces experimental configurable generic derivation in the circe-generic-extras
module).

## Fully automatic derivation

It is also possible to derive `Encoder`s and `Decoder`s for many types with no boilerplate at all.
circe uses [shapeless][shapeless] to automatically derive the necessary type class instances:

```tut:book
import io.circe.generic.auto._

case class Person(name: String)
case class Greeting(salutation: String, person: Person, exclamationMarks: Int)

Greeting("Hey", Person("Chris"), 3).asJson
```

## Custom encoders/decoders

If you want to write your own codec instead of using automatic or semi-automatic derivation, you can
do so in a couple of ways.

Firstly, you can write a new `Encoder[A]` and `Decoder[A]` from scratch:

```tut:book
class Thing(val foo: String, val bar: Int)

implicit val encodeFoo: Encoder[Thing] = new Encoder[Thing] {
  final def apply(a: Thing): Json = Json.obj(
    ("foo", Json.fromString(a.foo)),
    ("bar", Json.fromInt(a.bar))
  )
}

implicit val decodeFoo: Decoder[Thing] = new Decoder[Thing] {
  final def apply(c: HCursor): Decoder.Result[Thing] =
    for {
      foo <- c.downField("foo").as[String]
      bar <- c.downField("bar").as[Int]
    } yield {
      new Thing(foo, bar)
    }
}
```

But in many cases you might find it more convenient to piggyback on top of the decoders that are
already available. For example, a codec for `java.time.Instant` might look like this:

```tut:book
import cats.syntax.either._
import java.time.Instant

implicit val encodeInstant: Encoder[Instant] = Encoder.encodeString.contramap[Instant](_.toString)

implicit val decodeInstant: Decoder[Instant] = Decoder.decodeString.emap { str =>
  Either.catchNonFatal(Instant.parse(str)).leftMap(t => "Instant")
}
```

## Custom key types

If you need to encode/decode `Map[K, V]` where `K` is not `String` (or `Symbol`, `Int`, `Long`, etc.),
you need to provide a `KeyEncoder` and/or `KeyDecoder` for your custom key type.

For example:

```tut:book
import io.circe.syntax._

case class Foo(value: String)

implicit val fooKeyEncoder = new KeyEncoder[Foo] {
  override def apply(foo: Foo): String = foo.value
}
val map = Map[Foo, Int](
  Foo("hello") -> 123,
  Foo("world") -> 456
)

val json = map.asJson

implicit val fooKeyDecoder = new KeyDecoder[Foo] {
  override def apply(key: String): Option[Foo] = Some(Foo(key))
}

json.as[Map[Foo, Int]]
```

## Warnings and known issues

1. Please note that generic derivation will not work on Scala 2.10 unless you've added the [Macro
   Paradise][paradise] plugin to your build. See the [quick start section on the home page]({{ site.baseurl }}/index.html#quick-start)
   for details.

2. Generic derivation may not work as expected when the type definitions that you're trying to
   derive instances for are at the same level as the attempted derivation. For example:

   ```
   scala> import io.circe.Decoder, io.circe.generic.auto._
   import io.circe.Decoder
   import io.circe.generic.auto._

   scala> sealed trait A; case object B extends A; object X { val d = Decoder[A] }
   defined trait A
   defined object B
   defined object X

   scala> object X { sealed trait A; case object B extends A; val d = Decoder[A] }
   <console>:19: error: could not find implicit value for parameter d: io.circe.Decoder[X.A]
          object X { sealed trait A; case object B extends A; val d = Decoder[A] }
   ```

   This is unfortunately a limitation of the macro API that Shapeless uses to derive the generic
   representation of the sealed trait. You can manually define these instances, or you can arrange
   the sealed trait definition so that it is not in the same immediate scope as the attempted
   derivation (which is typically what you want, anyway).

3. For large or deeply-nested case classes and sealed trait hierarchies, the generic derivation
   provided by the `generic` subproject may stack overflow during compilation, which will result in
   the derived encoders or decoders simply not being found. Increasing the stack size available to
   the compiler (e.g. with `sbt -J-Xss64m` if you're using SBT) will help in many cases, but we have
   at least [one report][very-large-adt] of a case where it doesn't.

4. More generally, the generic derivation provided by the `generic` subproject works for a wide
   range of test cases, and is likely to _just work_ for you, but it relies on macros (provided by
   Shapeless) that rely on compiler functionality that is not always perfectly robust
   ("[SI-7046][si-7046] is like [playing roulette][si-7046-roulette]"), and if you're running into
   problems, it's likely that they're not your fault. Please file an issue here or ask a question on
   the [Gitter channel][gitter], and we'll do our best to figure out whether the problem is
   something we can fix.

5. When using the `io.circe.generic.JsonCodec` annotation, the following will not compile:

   ```scala
   import io.circe.generic.JsonCodec

   @JsonCodec sealed trait A
   case class B(b: String) extends A
   case class C(c: Int) extends A
   ```

   In cases like this it's necessary to define a companion object for the root type _after_ all of
   the leaf types:

   ```scala
   import io.circe.generic.JsonCodec

   @JsonCodec sealed trait A
   case class B(b: String) extends A
   case class C(c: Int) extends A

   object A
   ```

   See [this issue][circe-251] for additional discussion (this workaround may not be necessary in
   future versions).

6. circe's representation of numbers is designed not to lose precision during decoding into integral
   or arbitrary-precision types, but precision may still be lost during parsing. This shouldn't
   happen when using Jawn for parsing, but `scalajs.js.JSON` parses JSON numbers into a floating
   point representation that may lose precision (even when decoding into a type like `BigDecimal`;
   see [this issue][circe-262] for an example).

{% include references.md %}
