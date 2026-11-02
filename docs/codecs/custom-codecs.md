Custom Codecs
=============

### Custom encoders/decoders

If you want to write your own codec instead of using automatic or semi-automatic derivation, you can do so in a couple of ways.

Firstly, you can write a new `Encoder[A]` and `Decoder[A]` from scratch:

```scala mdoc
import io.circe.{ Decoder, Encoder, HCursor, Json }

class Thing(val foo: String, val bar: Int)

implicit val encodeFoo: Encoder[Thing] = 
  Encoder.instance[Thing] { a =>
    Json.obj(
      ("foo", Json.fromString(a.foo)),
      ("bar", Json.fromInt(a.bar))
    )
  }

implicit val decodeFoo: Decoder[Thing] =
  Decoder.instance[Thing] { c =>
    for {
      foo <- c.downField("foo").as[String]
      bar <- c.get[Int]("bar")
    } yield new Thing(foo, bar)
  }
```

  If want to have decoders that support accumulating errors, you can do so using the same `as` and `get` type functions above, but with a slight twist.
  
```scala mdoc
import cats.syntax.all._
implicit val decodeFoo2: Decoder[Thing] = 
  Decoder.accumulatingInstance[Thing] { c =>
    (
      c.downField("foo").asAcc[String],
      c.getAcc[Int]("bar")
    ).mapN { (foo, bar) =>
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

implicit val encodeInstant: Encoder[Instant] = 
  Encoder.encodeString.contramap[Instant](_.toString)

implicit val decodeInstant: Decoder[Instant] = 
  Decoder.decodeString.emapTry { str =>
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

implicit val fooKeyEncoder: KeyEncoder[Foo] = 
  KeyEncoder.instance[Foo] { (foo: Foo) =>
    foo.value
  }
  
val map = Map[Foo, Int](
  Foo("hello") -> 123,
  Foo("world") -> 456
)

val json = map.asJson

implicit val fooKeyDecoder: KeyDecoder[Foo] = 
  KeyDecoder.instance[Foo] { (key: String) =>
    Some(Foo(key))
  }

json.as[Map[Foo, Int]]
```
### Custom configuration
It's often necessary to customize the way encoding/decoding is made. 
For example, you may work with JSON objects which don't follow idiomatic case class member
names in Scala. By default, circe expects both sides to be named exactly the same.

You have few options here, usually depending on your scala version:
1. Scala versions 2.x : the standard generic derivation doesn't support this use case. However, you may use the experimental `generic-extras` external module.
2. Scala versions 3.x : the standard generic derivation provides a built-in support.
3. Version-independent alternative: explicit mapping (with a little boilerplate..)

Each option will be briefly presented in the following sections.
#### Key mappings via annotations - generic-extras

The experimental `generic-extras` module provides two ways to transform your case class member
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


#### More configuration arguments - generic-extras

Above we've seen how you can use `transformMemberNames` if the name of case class member names are different from the JSON keys.

Here's how you can use `transformConstructorNames` when encoding/decoding ADTs:

```scala mdoc:reset
import io.circe.generic.extras._
import io.circe.generic.extras.auto._
import io.circe.parser._

implicit val config: Configuration = Configuration.default.copy(
  transformConstructorNames = _.toLowerCase
)

sealed trait Animal
case class Dog(age: Int) extends Animal
case class Cat(color: String) extends Animal

decode[Animal]("""{"dog": {"age": 2}}""")
decode[Animal]("""{"cat": {"color": "brown"}}""")
```

If you want to allow default values when a field is missing from the JSON, you can use `useDefaults`:

```scala mdoc:reset
import io.circe.generic.extras._
import io.circe.generic.extras.auto._
import io.circe.parser._

implicit val config: Configuration = Configuration.default.copy(
  useDefaults = true
)

case class User(firstName: String, lastName: String = "Doe")

decode[User]("""{"firstName": "Foo"}""")
```

If `useDefaults = false`, the decoding would fail.

See [here](https://circe.github.io/circe/codecs/adt.html#the-future) for a use of `discriminator`.

And finally, we have `strictDecoding`.

By default, if a JSON has extra fields, we decode it without errors:

```scala mdoc:reset
import io.circe.generic.extras._
import io.circe.generic.extras.auto._
import io.circe.parser._

implicit val config: Configuration = Configuration.default

case class User(firstName: String, lastName: String)

decode[User]("""{"firstName": "Foo", "lastName": "Bar", "likesCats": true}""")
```

But we can be more strict, if we want to:

```scala mdoc:reset
import io.circe.generic.extras._
import io.circe.generic.extras.auto._
import io.circe.parser._

implicit val config: Configuration = Configuration.default.copy(
  strictDecoding = true
)

case class User(firstName: String, lastName: String)

decode[User]("""{"firstName": "Foo", "lastName": "Bar", "likesCats": true}""")
```

#### Custom configuration - Scala 3.x
Scala 3 brings in a built-in configuration possibilities without the need of an external `generic-extras` module.

Given the sample JSON:
```
{
  "name" : "Sir Meows",
  "lives_remaining" : 9,
  "humans_owned" : 4,
  "breed_type" : "Sphinx"
}
```

The configuration in Scala may look as follows:

```scala mdoc:reset
import io.circe.*
import io.circe.derivation.*

given Configuration = Configuration.default
  .withSnakeCaseMemberNames
  .withDiscriminator("breed_type")
  .withoutStrictDecoding //makes sure decoding doesn't fail due to lives_remaining

sealed trait Cat derives ConfiguredCodec

case class Sphinx(
  name: String,
  humansOwned: Int
) extends Cat

case class MaineCoon(
  name: String,
  humansOwned: Int,
  floofFactor: Int
) extends Cat
```
In case your case is simpler and/or you want to provide custom configuration per model, you may choose to put it into the companion object:

```scala mdoc:reset
import io.circe.*
import io.circe.derivation.*

case class Cat(name: String, humansOwned: Int)

object Cat {
  given Configuration = Configuration.default
    .withSnakeCaseMemberNames
    .withoutStrictDecoding

  given Codec[Cat] = ConfiguredCodec.derived
}
```
#### Alternative - explicit mapping
It's worth noting that if you need to handle just the difference between naming conventions, the
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
Also, this alternative will work regardless of your Scala version.
