---
layout: default
title:  "Traversing and modifying JSON"
section: "cursors"
---

# Traversing and modifying JSON

Working with JSON in circe usually involves using a cursor. Cursors are used both for extracting
data and for performing modification.

Suppose we have the following JSON document:

```scala
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

```scala
val cursor: HCursor = doc.hcursor
```

We can then use [various operations][generic-cursor] to move the focus of the cursor around the
document and extract data from it:

```scala
val baz: Decoder.Result[Double] =
  cursor.downField("values").downField("baz").as[Double]
// baz: io.circe.Decoder.Result[Double] = Right(100.001)

// You can also use `get[A](key)` as shorthand for `downField(key).as[A]`
val baz2: Decoder.Result[Double] =
  cursor.downField("values").get[Double]("baz")
// baz2: io.circe.Decoder.Result[Double] = Right(100.001)

val secondQux: Decoder.Result[String] =
  cursor.downField("values").downField("qux").downArray.right.as[String]
// secondQux: io.circe.Decoder.Result[String] = Right(b)
```

## Transforming data

We can also use a cursor to modify JSON. 

```scala
val reversedNameCursor: ACursor =
  cursor.downField("name").withFocus(_.mapString(_.reverse))
```

We can then return to the root of the document and return its value with `top`:

```scala
val reversedName: Option[Json] = reversedNameCursor.top
// reversedName: Option[io.circe.Json] =
// Some({
//   "id" : "c730433b-082c-4984-9d66-855c243266f0",
//   "name" : "ooF",
//   "counts" : [
//     1,
//     2,
//     3
//   ],
//   "values" : {
//     "bar" : true,
//     "baz" : 100.001,
//     "qux" : [
//       "a",
//       "b"
//     ]
//   }
// })
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
