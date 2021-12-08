---
layout: docs
title:  "Parsing JSON"
position: 1
---

# Parsing JSON

circe includes a parsing module, which on the JVM is a wrapper around the [Jawn][jawn] JSON parser and for JavaScript uses the built-in [`JSON.parse`][json.parse].

Parsing is not part of the `circe-core` module, so you will need to include a dependency on the `circe-parser` module in your build:

```scala
libraryDependencies += "io.circe" %% "circe-parser" % circeVersion
```

Parsing is done as follows.

```scala mdoc
import io.circe._, io.circe.parser._

val rawJson: String = """
{
  "foo": "bar",
  "baz": 123,
  "list of stuff": [ 4, 5, 6 ]
}
"""

val parseResult = parse(rawJson)
```

Because parsing might fail, the result is an `Either` with an `io.circe.Error` on the left side.
In the example above, the input was valid JSON, so the result was a `Right` containing the
corresponding JSON representation.

Let's see what happens when you try to parse invalid JSON:

```scala mdoc
val badJson: String = "yolo"

parse(badJson)
```

There are a number of ways to extract the parse result from the `Either`. For example you could pattern
match on it:

```scala mdoc
parse(rawJson) match {
  case Left(failure) => println("Invalid JSON :(")
  case Right(json) => println("Yay, got some JSON!")
}
```

Or use `getOrElse` (an extension method provided by Cats):

```scala mdoc
val json: Json = parse(rawJson).getOrElse(Json.Null)
```

## Warnings and known issues

When using the Scala.js version of circe, numerical values like `Long` may [lose
precision][#393] when decoded. For example `decode[Long]("767946224062369796")`
will return `Right(767946224062369792L)`. This is not a limitation of how
Scala.js represents `scala.Long`s nor circe's decoders for numerical values but
due to [`JSON.parse`][json.parse] converting numerical values to JavaScript
numbers. If precision is required consider representing numerical values as
strings and convert them to their final value via the JSON AST.

 [#393]: https://github.com/circe/circe/issues/393

{% include references.md %}
