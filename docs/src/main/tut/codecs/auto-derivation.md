---
layout: docs
title:  "Automatic derivation"
---

### Automatic Derivation

It is also possible to derive `Encoder`s and `Decoder`s for many types with no boilerplate at all. Circe uses [shapeless](https://github.com/milessabin/shapeless) to automatically derive the necessary type class instances:

```tut:book
import io.circe.generic.auto._, io.circe.syntax._

case class Person(name: String)
case class Greeting(salutation: String, person: Person, exclamationMarks: Int)

Greeting("Hey", Person("Chris"), 3).asJson
```
