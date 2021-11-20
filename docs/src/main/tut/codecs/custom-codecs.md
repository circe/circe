---
layout: docs
title:  "Custom codecs"
---

### Custom encoders/decoders

If you want to write your own codec instead of using automatic or semi-automatic derivation, you can do so in a couple of ways.

Firstly, you can write a new `Encoder[A]` and `Decoder[A]` from scratch:

```scala mdoc
import io.circe.{ Decoder, Encoder, HCursor, Json }

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

```scala mdoc
import io.circe.{ Decoder, Encoder }
import java.time.Instant
import scala.util.Try

implicit val encodeInstant: Encoder[Instant] = Encoder.encodeString.contramap[Instant](_.toString)

implicit val decodeInstant: Decoder[Instant] = Decoder.decodeString.emapTry { str =>
  Try(Instant.parse(str))
}
```

#### Older scala versions

If you are using custom codecs and an older versions of scala (below 2.12) and you get errors like 
this `value flatMap is not a member of io.circe.Decoder.Result[Option[String]]` or 
`value map is not a member of io.circe.Decoder.Result[Option[String]]` then you need to use the 
following import: `import cats.syntax.either._` to fix this.

### Custom key types

If you need to encode/decode `Map[K, V]` where `K` is not `String` (or `Symbol`, `Int`, `Long`, etc.),
you need to provide a `KeyEncoder` and/or `KeyDecoder` for your custom key type.

For example:

```scala mdoc
import io.circe._, io.circe.syntax._

case class Foo(value: String)

implicit val fooKeyEncoder: KeyEncoder[Foo] = new KeyEncoder[Foo] {
  override def apply(foo: Foo): String = foo.value
}
val map = Map[Foo, Int](
  Foo("hello") -> 123,
  Foo("world") -> 456
)

val json = map.asJson

implicit val fooKeyDecoder: KeyDecoder[Foo] = new KeyDecoder[Foo] {
  override def apply(key: String): Option[Foo] = Some(Foo(key))
}

json.as[Map[Foo, Int]]
```

### Custom key mappings via annotations

It's often necessary to work with keys in your JSON objects that aren't idiomatic case class member
names in Scala. While the standard generic derivation doesn't support this use case, the
experimental `generic-extras` module does provide two ways to transform your case class member
names during encoding and decoding.

In many cases the transformation is as simple as going from camel case to snake case, in which case
all you need is a custom implicit configuration:

```scala mdoc
import io.circe.generic.extras._, io.circe.syntax._

implicit val config: Configuration = Configuration.default.withSnakeCaseMemberNames

@ConfiguredJsonCodec case class User(firstName: String, lastName: String)

User("Foo", "McBar").asJson
```

In other cases you may need more complex mappings. These can be provided as a function:

```scala mdoc:reset
import io.circe.generic.extras._, io.circe.syntax._

implicit val config: Configuration = Configuration.default.copy(
  transformMemberNames = {
    case "i" => "my-int"
    case other => other
  }
)

@ConfiguredJsonCodec case class Bar(i: Int, s: String)

Bar(13, "Qux").asJson
```

Since this is a common use case, we also support for mapping member names via an annotation:

```scala mdoc:reset
import io.circe.generic.extras._, io.circe.syntax._

implicit val config: Configuration = Configuration.default

@ConfiguredJsonCodec case class Bar(@JsonKey("my-int") i: Int, s: String)

Bar(13, "Qux").asJson
```

It's worth noting that if you don't want to use the `generic-extras` module, the
completely unmagical `forProductN` version isn't really that much of a burden:

```scala mdoc:reset
import io.circe.Encoder, io.circe.syntax._

case class User(firstName: String, lastName: String)
case class Bar(i: Int, s: String)

implicit val encodeUser: Encoder[User] =
  Encoder.forProduct2("first_name", "last_name")(u => (u.firstName, u.lastName))

implicit val encodeBar: Encoder[Bar] =
  Encoder.forProduct2("my-int", "s")(b => (b.i, b.s))

User("Foo", "McBar").asJson
Bar(13, "Qux").asJson
```

While this version does involve a bit of boilerplate, it only requires `circe-core`, and may have slightly better runtime performance in some cases.
