---
layout: docs
title:  "Codec testing"
---

### Codec testing

Suppose you have the following `Person` case class and hand-written encoders and decoders. In this
case, your decoder includes a typo, `"mame"` instead of `"name"`.

```scala mdoc
import io.circe._
import io.circe.syntax._

case class Person(name: String)
object Person {
  implicit val encPerson: Encoder[Person] = Encoder.forProduct1("name")(_.name)
  implicit val decPerson: Decoder[Person] = Decoder.forProduct1("mame")(Person.apply _)
}
```

If you try to encode then decode a `Person`, you won't be successful:

```scala mdoc
Person("James").asJson.as[Person]
```

This process is an example of a round trip. We can think about this round trip at two
different levels. Thinking about it for the particular case class `Person`, we can say
that any `Person` should round trip. This is a property we expect about the `Person`
case class.

However, this is a common expectation for anything that has both an `Encoder` and `Decoder`.
In that sense, round tripping is a property we expect about anything with a `Codec`. When
we have expectations about all things that implement some typeclass, we have a _law_.

### `Codec` laws

To check `Codec` laws for your custom types, they'll need two implicits in scope -- `Arbitrary` and
`Eq`.

```scala mdoc
import cats.Eq
import io.circe.testing.ArbitraryInstances
import org.scalacheck.{Arbitrary, Gen}

object Implicits extends ArbitraryInstances {
  implicit val eqPerson: Eq[Person] = Eq.fromUniversalEquals
  implicit val arbPerson: Arbitrary[Person] = Arbitrary {
    Gen.listOf(Gen.alphaChar) map { chars => Person(chars.mkString("")) }
  }
}
```

The presence of those implicit values and an import from the `circe-testing` module
will allow you to create a `CodecTests[Person]`:

```scala mdoc
import io.circe.testing.CodecTests

val personCodecTests = CodecTests[Person]
```

`CodecTests[T]` expose two ["rule sets"](https://typelevel.org/blog/2013/11/17/discipline.html#interface)
that can be used with [`Discipline`](https://github.com/typelevel/discipline). The less restrictive set
is `unserializableCodec`.

```scala mdoc
import Implicits._
personCodecTests.unserializableCodec
```

It checks whether the `Codec` for your type successfully round trips through json serialization and
deserialization and whether your decoder satisfies consistent error accumulation.

The more restrictive set is `codec`:

```scala mdoc
personCodecTests.codec
```

It checks the laws from `unserializableCodec` and ensures that your encoder and decoder can be serialized
to and from Java byte array streams. It is generally a good idea to use the stronger laws from `.codec`, and
you definitely should use them if you're in a setting where the JVM has to ship a lot of data around, for
example in a Spark application. However, if you're not in a distributed setting and the serializability laws
are getting in your way, it's fine to skip them with the `unserializableCodec`.
