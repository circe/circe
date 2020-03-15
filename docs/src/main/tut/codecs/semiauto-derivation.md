---
layout: docs
title:  "Semi-automatic derivation"
---

### Semi-automatic Derivation

Sometimes it's convenient to have an `Encoder` or `Decoder` defined in your code, and semi-automatic derivation can help. You'd write:

```scala mdoc:silent
import io.circe._, io.circe.generic.semiauto._

case class Foo(a: Int, b: String, c: Boolean)

implicit val fooDecoder: Decoder[Foo] = deriveDecoder[Foo]
implicit val fooEncoder: Encoder[Foo] = deriveEncoder[Foo]
```

Or simply:

```scala mdoc:silent:reset
import io.circe._, io.circe.generic.semiauto._

case class Foo(a: Int, b: String, c: Boolean)

implicit val fooDecoder: Decoder[Foo] = deriveDecoder
implicit val fooEncoder: Encoder[Foo] = deriveEncoder
```

### @JsonCodec

The circe-generic project includes a `@JsonCodec` annotation that simplifies the
use of semi-automatic generic derivation:

```scala mdoc
import io.circe.generic.JsonCodec, io.circe.syntax._

@JsonCodec case class Bar(i: Int, s: String)

Bar(13, "Qux").asJson
```

This works with both case classes and sealed trait hierarchies.

NOTE: You will need the [Macro Paradise](https://docs.scala-lang.org/overviews/macros/paradise.html) plugin to use annotation macros like `@JsonCodec`

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

It's not as clean or as maintainable as generic derivation, but it's less magical, it requires nothing but `circe-core`, and if you need a custom name mapping it's currently the best solution (although `0.6.0` introduces experimental configurable generic derivation in the `circe-generic-extras` module).

### Support for Value Classes

There's also a support for Value Classes. It's available within `circe-generic-extras`:

```scala mdoc
import io.circe.Encoder
import io.circe.syntax._
import io.circe.generic.extras.semiauto._
import io.circe.generic.extras.defaults._

implicit val ProfileEncoder: Encoder[Profile]   = deriveConfiguredEncoder
implicit val UserIdEncoder: Encoder[UserId]     = deriveUnwrappedEncoder
implicit val UserNameEncoder: Encoder[UserName] = deriveUnwrappedEncoder

case class UserId(id: Int) extends AnyVal
case class UserName(name: String) extends AnyVal
case class Profile(userId: UserId, userName: UserName)

val pr = Profile(UserId(1446), UserName("User Name"))

pr.asJson

```
