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

```scala mdoc
import io.circe.syntax._

val intsJson = List(1, 2, 3).asJson
```

Use the `.as` syntax for decoding data from `Json`:

```scala mdoc
intsJson.as[List[Int]]
```

The `decode` function from the included [parser] module can be used to directly decode
a JSON `String`:

```scala mdoc
import io.circe.parser.decode

decode[List[Int]]("[1, 2, 3]")
```

{% include references.md %}
