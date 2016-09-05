---
layout: default
title:  "Optics"
section: "optics"
---

# Optics

Optics are a powerful tool for traversing and modifying JSON documents. They can reduce boilerplate 
considerably, especially if you are working with deeply nested JSON.

circe provides support for optics by integrating with [Monocle](monocle). To use them, add a
dependency on `circe-optics` to your build:

```scala
libraryDependencies += "io.circe" %% "circe-optics" % circeVersion
```

Note that this will require your project to depend on both Scalaz and cats.

## Traversing JSON

Suppose we have the following JSON document:

```scala
import io.circe._, io.circe.parser._

val json: Json = parse("""
{
  "order": {
    "customer": {
      "name": "Custy McCustomer",
      "contactDetails": {
        "address": "1 Fake Street, London, England",
        "phone": "0123-456-789"
      }
    },
    "items": [{
      "id": 123,
      "description": "banana",
      "quantity": 1
    }, {
      "id": 456,
      "description": "apple",
      "quantity": 2
    }],
    "total": 123.45
  }
}
""").getOrElse(Json.Null)
```

If we wanted to get the customer's phone number, we could do it using a cursor as follows:

```scala
val phoneNum: Option[String] = json.hcursor.
      downField("order").
      downField("customer").
      downField("contactDetails").
      get[String]("phone").
      toOption
// phoneNum: Option[String] = Some(0123-456-789)
```

This works, but it's a little verbose. We could rewrite it using optics like this:

```scala
import io.circe.optics.JsonPath._
// import io.circe.optics.JsonPath._

val _phoneNum = root.order.customer.contactDetails.phone.string
// _phoneNum: monocle.Optional[io.circe.Json,String] = monocle.POptional$$anon$1@52e8106d

val phoneNum: Option[String] = _phoneNum.getOption(json)
// phoneNum: Option[String] = Some(0123-456-789)
```

Note the difference between cursors and optics. With cursors, we start with a JSON document, get a 
cursor from it, and then use that cursor to traverse the document. With optics, on the other hand, 
we first define the traversal we want to make, then apply it to a JSON document.

In other words, optics provide a way to separate the description of a JSON traversal from its
execution. Consequently we can reuse the same traversal against many different documents, compose
traversals together, and so on.

Let's look at a more complex example. This time we want to get the quantities of all the
items in the order. Using a cursor it might look like this:

```scala
val items: List[Json] = json.hcursor.
      downField("order").
      downField("items").
      focus.
      flatMap(_.asArray).
      getOrElse(Nil)
// items: List[io.circe.Json] =
// List({
//   "id" : 123,
//   "description" : "banana",
//   "quantity" : 1
// }, {
//   "id" : 456,
//   "description" : "apple",
//   "quantity" : 2
// })

val quantities: Seq[Int] =
  items.flatMap(_.cursor.get[Int]("quantity").toOption)
// quantities: Seq[Int] = List(1, 2)
```

And with optics:

```scala
val items: Seq[Int] = 
  root.order.items.each.quantity.int.getAll(json)
// items: Seq[Int] = List(1, 2)
```

## Modifying JSON

Optics can also be used for making modifications to JSON.

Suppose we decide to have a 2-for-1 sale, so we want to double all the quantities in the order. This
can be achieved with a small change to the code we wrote for traversal:

```scala
val doubleQuantities: Json => Json = 
  root.order.items.each.quantity.int.modify(_ * 2)
// doubleQuantities: io.circe.Json => io.circe.Json = <function1>

val modifiedJson = doubleQuantities(json)
// modifiedJson: io.circe.Json =
// {
//   "order" : {
//     "customer" : {
//       "name" : "Custy McCustomer",
//       "contactDetails" : {
//         "address" : "1 Fake Street, London, England",
//         "phone" : "0123-456-789"
//       }
//     },
//     "items" : [
//       {
//         "id" : 123,
//         "description" : "banana",
//         "quantity" : 2
//       },
//       {
//         "id" : 456,
//         "description" : "apple",
//         "quantity" : 4
//       }
//     ],
//     "total" : 123.45
//   }
// }
```

The result is a copy of the original JSON with only the `quantity` fields updated.

## Dynamic

Some of the code above may look quite magical at first glance. How are we calling methods like
`order`, `items` and `customer` on circe's [JsonPath][jsonpath] class?

The answer is that `JsonPath` relies on a slightly obscure feature of Scala called `Dynamic`. This
means you can call methods that don't actually exist. When you do so, the `selectDynamic` method is
called, and the name of the method you wanted to call is passed as an argument.

### Warning

The use of Dynamic means that your code is not "typo-safe". For example, if you fat-finger the previous
example:

```scala
val doubleQuantities: Json => Json =
  root.order.itemss.each.quantity.int.modify(_ * 2) // Note the "itemss" typo

val modifiedJson = doubleQuantities(json)
```

This code will compile just fine, but not do what you expect. Because the JSON document doesn't have
an `itemss` field, the same document will be returned unmodified.

{% include references.md %}
