---
layout: default
title:  "Parsing JSON"
section: "parsing"
---

# Parsing JSON

Circe includes a parsing module, which is a wrapper around the [Jawn][jawn] JSON parser.

Parsing is not part of the `circe-core` module, so you will need to include a dependency on the `circe-parser` module in your build:

```scala
libraryDependencies += "io.circe" %% "circe-parser" % circeVersion
```

Parsing is done as follows.

```tut:book
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

Because parsing might fail, the result is a cats [Xor][cats-xor]. In the example above, the input
was valid JSON, so the result was a `Right` containing the corresponding JSON representation.

Let's see what happens when you try to parse invalid JSON:

```tut:book
val badJson: String = "yolo"

parse(badJson)
```

There are a number of ways to extract the parse result from the `Xor`. For example you could pattern
match on it:

```tut:book
import cats.data.Xor._

parse(rawJson) match {
  case Left(failure) => println("Invalid JSON :(")
  case Right(json) => println("Yay, got some JSON!")
}
```

Or use `getOrElse`:

```tut:book
val json: Json = parse(rawJson).getOrElse(Json.Null)
```

{% include references.md %}
