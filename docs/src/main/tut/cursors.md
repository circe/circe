---
layout: docs
title:  "Traversing and modifying JSON"
position: 2
---

# Traversing and modifying JSON

Working with JSON in circe usually involves using a cursor. Cursors are used both for extracting
data and for performing modification.

Suppose we have the following JSON document:

```tut:silent
import cats.syntax.either._
import io.circe._, io.circe.parser._

val json: String = """
  {
    "id": "c730433b-082c-4984-9d66-855c243266f0",
    "name": "Foo",
    "counts": [1, 2, 3],
    "values": {
      "bar": true,
      "baz": 100.001,
      "qux": ["a", "b"]
    }
  }
"""

val doc: Json = parse(json).getOrElse(Json.Null)
```

## Extracting data

In order to traverse the document we need to create an `HCursor` with the focus at the document's
root:

```tut:silent
val cursor: HCursor = doc.hcursor
```

We can then use [various operations][circe-cursor] to move the focus of the cursor around the
document and extract data from it:

```tut:book
val baz: Decoder.Result[Double] =
  cursor.downField("values").downField("baz").as[Double]

// You can also use `get[A](key)` as shorthand for `downField(key).as[A]`
val baz2: Decoder.Result[Double] =
  cursor.downField("values").get[Double]("baz")

val secondQux: Decoder.Result[String] =
  cursor.downField("values").downField("qux").downArray.as[String]
```

## Transforming data

We can also use a cursor to modify JSON.

```tut:silent
val reversedNameCursor: ACursor =
  cursor.downField("name").withFocus(_.mapString(_.reverse))
```

We can then return to the root of the document and return its value with `top`:

```tut:book
val reversedName: Option[Json] = reversedNameCursor.top
```

The result contains the original document with the `"name"` field reversed.

Note that `Json` is immutable, so the original document is left unchanged.

## Cursors

circe has three slightly different cursor implementations:

* `Cursor` provides functionality for moving around a tree and making modifications
* `HCursor` tracks the history of operations performed. This can be used to provide useful error messages when something goes wrong.
* `ACursor` also tracks history, but represents the possibility of failure (e.g. calling `downField` on a field that doesn't exist)

## Optics

Optics are an alternative way to traverse JSON documents. See the [Optics page](optics.html) for
more details.

{% include references.md %}
