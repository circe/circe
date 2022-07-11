---
layout: docs
title:  "Semi-automatic derivation"
---

### Semi-automatic Derivation

Sometimes it's convenient to have an `Encoder` or `Decoder` defined in your code, and semi-automatic derivation can help. You'd write:

```scala mdoc:silent
import io.circe._, io.circe.generic.semiauto._, io.circe.syntax._

case class Foo(a: Int, b: String, c: Boolean)

implicit val fooDecoder: Decoder[Foo] = deriveDecoder[Foo]
implicit val fooEncoder: Encoder[Foo] = deriveEncoder[Foo]

Foo(13, "Qux", false).asJson
```

You can also simplify to just write the last two lines as:

```scala mdoc:silent:reset
implicit val fooDecoder: Decoder[Foo] = deriveDecoder
implicit val fooEncoder: Encoder[Foo] = deriveEncoder
```

#### Specific case for Value Class

Most of the time, when using Value Class, we expect only the inner value in the serialized format.

It can be achieved using `circe-generic-extras`:

```scala mdoc:silent:reset
import io.circe._, io.circe.generic.extras.semiauto._

case class Foo(a: Int)

implicit val fooDecoder: Decoder[Foo] = deriveUnwrappedDecoder[Foo]
implicit val fooEncoder: Encoder[Foo] = deriveUnwrappedEncoder[Foo]
```

The expected serialization for `Foo(123)` is simply `123`

### @JsonCodec

The `circe-generic` project includes a `@JsonCodec` annotation that simplifies the
use of semi-automatic generic derivation:

```scala mdoc
import io.circe.generic.JsonCodec, io.circe.syntax._

@JsonCodec case class Bar(i: Int, s: String)

Bar(13, "Qux").asJson
```

This works with both case classes and sealed trait hierarchies.

NOTE: You will need to use the `-Ymacro-annotations` flag to use [annotation macros](https://docs.scala-lang.org/overviews/macros/annotations.html) like `@JsonCodec`. (If you're using Scala 2.10.x to Scala 2.12.x you will need the [Macro Paradise](https://docs.scala-lang.org/overviews/macros/paradise.html) plugin instead).

### forProductN helper methods

It's also possible to construct encoders and decoders for case class-like types
in a relatively boilerplate-free way without generic derivation:

```scala mdoc
import io.circe.{ Decoder, Encoder }

case class User(id: Long, firstName: String, lastName: String)

implicit val decodeUser: Decoder[User] =
  Decoder.forProduct3("id", "first_name", "last_name")(User.apply)

implicit val encodeUser: Encoder[User] =
  Encoder.forProduct3("id", "first_name", "last_name")(u =>
    (u.id, u.firstName, u.lastName)
  )
```

It's not as clean or as maintainable as generic derivation, but it's less magical, it requires nothing but `circe-core`, and if you need a custom name mapping it's currently the best solution (although `0.6.0` introduces experimental configurable generic derivation in the `generic-extras` module).
