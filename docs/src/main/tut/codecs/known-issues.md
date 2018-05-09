---
layout: docs
title:  "Warnings and known issues"
---

### Warnings and known issues

1. Please note that generic derivation will not work on Scala 2.10 unless you've added the [Macro
   Paradise][paradise] plugin to your build. See the [quick start section on the home page]({{ site.baseurl }}/index.html#quick-start)
   for details.

2. Generic derivation may not work as expected when the type definitions that you're trying to
   derive instances for are at the same level as the attempted derivation. For example:

   ```
   scala> import io.circe.Decoder, io.circe.generic.auto._
   import io.circe.Decoder
   import io.circe.generic.auto._

   scala> sealed trait A; case object B extends A; object X { val d = Decoder[A] }
   defined trait A
   defined object B
   defined object X

   scala> object X { sealed trait A; case object B extends A; val d = Decoder[A] }
   <console>:19: error: could not find implicit value for parameter d: io.circe.Decoder[X.A]
          object X { sealed trait A; case object B extends A; val d = Decoder[A] }
   ```

   This is unfortunately a limitation of the macro API that Shapeless uses to derive the generic
   representation of the sealed trait. You can manually define these instances, or you can arrange
   the sealed trait definition so that it is not in the same immediate scope as the attempted
   derivation (which is typically what you want, anyway).

3. For large or deeply-nested case classes and sealed trait hierarchies, the generic derivation
   provided by the `generic` subproject may stack overflow during compilation, which will result in
   the derived encoders or decoders simply not being found. Increasing the stack size available to
   the compiler (e.g. with `sbt -J-Xss64m` if you're using SBT) will help in many cases, but we have
   at least [one report][very-large-adt] of a case where it doesn't.

4. More generally, the generic derivation provided by the `generic` subproject works for a wide
   range of test cases, and is likely to _just work_ for you, but it relies on macros (provided by
   Shapeless) that rely on compiler functionality that is not always perfectly robust
   ("[SI-7046][si-7046] is like [playing roulette][si-7046-roulette]"), and if you're running into
   problems, it's likely that they're not your fault. Please file an issue here or ask a question on
   the [Gitter channel][gitter], and we'll do our best to figure out whether the problem is
   something we can fix.

5. When using the `io.circe.generic.JsonCodec` annotation, the following will not compile:

   ```scala
   import io.circe.generic.JsonCodec

   @JsonCodec sealed trait A
   case class B(b: String) extends A
   case class C(c: Int) extends A
   ```

   In cases like this it's necessary to define a companion object for the root type _after_ all of
   the leaf types:

   ```scala
   import io.circe.generic.JsonCodec

   @JsonCodec sealed trait A
   case class B(b: String) extends A
   case class C(c: Int) extends A

   object A
   ```

   See [this issue][circe-251] for additional discussion (this workaround may not be necessary in
   future versions).

6. circe's representation of numbers is designed not to lose precision during decoding into integral
   or arbitrary-precision types, but precision may still be lost during parsing. This shouldn't
   happen when using Jawn for parsing, but `scalajs.js.JSON` parses JSON numbers into a floating
   point representation that may lose precision (even when decoding into a type like `BigDecimal`;
   see [this issue][circe-262] for an example).

### `knownDirectSubclasses` error

While using fully automatic derivation, you may have run into an error that looks like this:

```scala
knownDirectSubclasses of <class> observed before subclass <class> registered
```

This is a known issue ([#434](https://github.com/circe/circe/issues/434), [#659](https://github.com/circe/circe/issues/639))
that stems from the way fully automatic derivation relies on Shapeless, which in turn conditionally
calls a Scala Reflect named called `knownDirectSubclasses`. This method has been known to fail depending
on how the types that it interacts with are declared in your codebase.

Here is a collection workarounds found by other users that you can try:

  1. Rename your files/directories so that the files containing types that get encoded/decoded come
     alphabetically before the files that `import io.circe.generic.auto._` and turn values of those
     types into JSON.
  2. If you've got a sealed trait (e.g. `sealed trait MyEnum`) and it has subclasses that are declared
     in its companion object, try adding a `import MyEnum._` statement before any calls that force the
     materialising of an encoder/decoder.

     ```scala
     import io.circe.syntax._
     import io.circe.generic.auto._
     val person = Person("hello", Role.User)
     import Role._
     val asJson = person.asJson
     ```

     * Alternatively, if you are OK with losing namespacing for your enum members you can try moving
       the subclasses out of the parent trait's companion object and into the same namespace space
       as the parent trait:

       ```scala
       // Modify this
       sealed trait ShirtSize

       object ShirtSize {
         case object Small  extends ShirtSize
         case object Medium extends ShirtSize
         case object Large  extends ShirtSize
       }

       // into this
       sealed trait ShirtSizes

       case object Small  extends ShirtSizes
       case object Medium extends ShirtSizes
       case object Large  extends ShirtSizes
       ```
   3. Try using Scala 2.11.8, the last known version of Scala that did not exhibit this problem.

If none of these workarounds are desirable for your use case, it might be a good idea to try semi-auto derivation instead.
