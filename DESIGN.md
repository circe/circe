# Design guidelines and justification

In this document I'll attempt to explain some of the design decisions that I've made in circe, and
to justify some of the gaps in the API where circe doesn't (currently) support the same kinds of
functionality you'll find in other Scala JSON libraries. I'm hoping to close all of these gaps, but
in most cases they exist because I've put constraints on the project that I don't want to relax
until I've experimented with other solutions.

## Disclaimer

While the wording here may come across as dogmatic, this isn't because I think these principles are
the "right" way to do things. These positions are an experiment that's informed by my individual
experience, and everything here is up for debate.

Please don't read this as a general Scala style guide. For one thing it's entirely about writing
library code and designing a library API, and some of it is JSON-specific, but it's also based on
lots of more-or-less well-considered personal preferences that are subject to change.

## Guidelines

1. **Your model code shouldn't (have to) know anything about serialization.**
    Sometimes it's appropriate to have serialization code in your model, and sometimes it isn't. As
    a serialization library, circe should not penalize the cases where it's not appropriate, and the
    same functionality should be available in both situations, with no more than a single extra
    import being required in the latter.

    What this means concretely: [MetaRest][metarest] is a very interesting project, and many
    features in circe are directly inspired by MetaRest, including [incomplete decoder
    derivation][incompletes]. circe is in a sense the opposite of MetaRest, though, and
    `circe-core` will never include e.g. annotations that are intended to be used on case class
    members to guide serialization.

2. **You generally shouldn't need or want to work with JSON ASTs directly.**
    This is part of the reason I couldn't personally care less about [SLIP-28][slip-28]: I think
    JSON is a horrible serialization format, and I want to help people not have to think about JSON.
    If you absolutely _have to_ work with JSON values directly, you at least shouldn't have to worry
    about keeping track of traversal history or manually handling modification of deeply nested
    structures (i.e. you should be using a relatively nice API like `ACursor` or the facilities
    supported by [Monocle][monocle] in `circe-optics`).

    What this means concretely: If something like SLIP-28 ever actually lands in the standard
    library, I'll probably adopt it (assuming it doesn't violate other design principles here—which
    is a huge assumption), but I hope most users won't even notice that change.

3. **Let a thousand modules bloom (at least until 1.0).**
    I'd prefer to err on the side of modularity, with major new functionality being introduced first
    in new sub-projects, especially if it requires new dependencies. I'd also prefer to be able to
    build modules together for now, which means there are a lot of sub-projects in the circe repo,
    and they aren't all necessarily at the same level of maturity (even though they share a version
    and are published together).

    At the 1.0 mark I plan to re-evaluate all sub-projects, and some may be spun off to separate
    projects (where they can be independently versioned), or just retired if they aren't being used
    or maintained.

    What this means concretely: The root directory of the project is kind of a zoo, and complete
    builds can take a while.

4. **Implicit scope should not be used for configuration.**
    Lots of people have asked for a way to configure generic codec derivation to use e.g. a `type`
    field as the discriminator for sealed trait hierarchies, or to use snake case for member names.
    [argonaut-shapeless][argonaut-shapeless] supports this quite straightforwardly with a
    [`JsonCoproductCodec`][argonaut-shapeless-7] type that the user can provide implicitly.

    I don't
    want to criticize this approach—it's entirely idiomatic Scala, and it often works well in
    practice—but I personally don't like using implicit values for configuration, and I'd like to
    avoid it in circe until I am 100% convinced that there's no alternative way to provide this
    functionality.

    What this means concretely: You'll probably never see an implicit argument that isn't a type
    class instance—i.e. that isn't a type constructor applied to a type in your model—in circe, and
    configuration of generic codec derivation is going to be relatively limited (compared to e.g.
    argonaut-shapeless) until we find a nice way to do this kind of thing with type tags or
    something similar.

5. **Implicit conversions aren't worth it.**
    In my experience implicit conversions are loans against your own (and your users') future
    understanding of your code.

    What this means concretely: Any proposed changes including implicit conversions will need to
    make a _very_ strong case for their necessity. There are two exceptions: conversions from one
    type class instance to another, and conversion to a syntax-enabling `Ops` class if for some
    reason it's not possible to use an implicit class (although see the following point).

6. **Syntactic extension via enrichment methods should be kept behind a `syntax` import.**
    This is line with the general principle that magic should be opt-in.

    What this means concretely: If you want to be able to write things like
    `Map("foo" -> List(1, 2, 3)).asJson`, you'll need to import `io.circe.syntax._`.

7. **Runtime reflection: not even once.**
    As [Rob Norris][tpolecat] [says][no-reflection]: "programs should be invariant under rename
    refactoring, therefore no reflection". See [this Stack Overflow answer][on-reflection] for more
    discussion, including a defense of _compile-time_ reflection.

    What this means concretely: It's unlikely you'll ever see an `Any` in circe except in code
    that's absolutely necessary for JVM or Scala.js compatibility.

8. **The public API should not contain unnecessary methods.**
    I'm all for [TMTOWTDI][tmtowtdi] at certain levels of abstraction, but I think redundancy in
    library API design is a terrible way to treat users (especially new ones).

    What this means concretely: If the standard way to create a successful `ACursor` from an
    `HCursor` is `ACursor.ok`, there should not be an equivalent `toACursor` method on `HCursor`.
    Don't even get me started on symbolic operator aliases.

9. **The public API should not contain unnecessary types.**
    Argonaut (like many other great serialization libraries) includes a `Codec` type class that
    combines `Encoder` and `Decoder`, but it's only really useful for definitions: it allows you to
    define an encoder-decoder pair together, but its generally not useful as a type constraint,
    since many types with both `Encoder` and `Decoder` instances will not have a `Codec` instance.
    I personally find this confusing, and would prefer to keep convenience-oriented types like this
    out of the API.

    What this means concretely: There's unlikely to be a `Codec` in circe until we come up with
    a [clean way][circe-codec] to make it useful as a type constraint (although if demand gets loud
    enough I would be willing to consider compromise here).

10. **Avoid variance, but without burdening users.**
    This isn't a popular decision (even Argonaut's codec type classes are [co- and contravariant
    now](argonaut-variance)), but I'm not convinced it's worth it, especially given the way that
    generic derivation works for ADTs (i.e. `Decoder[Base]` and `Decoder[Leaf]` behave differently).

    What this means concretely: You may occasionally need to upcast to get the right encoder to
    kick in.

11. **Not all ADTs should expose their constructors.**
    In some cases, including most significantly here the `io.circe.Json` type, we don't want to
    encourage users to think of the ADT leaves as having meaningful types. A JSON value "is" a
    boolean or a string or a unit or a `Seq[Json]` or a `JsonNumber` or a `JsonObject`. Introducing
    types like `JString`, `JNumber`, etc. into the public API just confuses things.

    What this means concretely: You can't deconstruct JSON values with pattern matching. Instead
    you'll need to use `fold`, the `asX` methods, etc.

12. **The library implementation does not model usage.**
    People often complain about how functional programming in Scala is painfully slow, and while
    there are reasons that this will be true (to some extent) for the foreseeable future, I think
    the problem is made worse than it needs to be by the approach that libraries like Scalaz and
    Argonaut take to the correctness-performance trade-off.

    In circe the public API is almost exclusively purely functional, but there is extensive use of
    mutability, casts, imperative constructions, etc. behind the scenes. While we want correctness,
    we believe that for a Scala library at this level, letting our tests verify correctness instead
    of the compiler may be necessary in some cases to achieve the performance we want.

    What this means concretely: "Idiomatic" usage needs to be modelled by example projects, not
    the library implementation. Unfortunately right now these example projects don't really exist,
    but we're working on it.

[argonaut-shapeless]: https://github.com/alexarchambault/argonaut-shapeless
[argonaut-shapeless-7]: https://github.com/alexarchambault/argonaut-shapeless/pull/7
[argonaut-variance]: https://github.com/argonaut-io/argonaut/blob/28953e2dac90ab9efb2db565491e1948d581aa01/argonaut/src/main/scala/argonaut/DecodeJson.scala#L9
[incompletes]: https://meta.plasm.us/posts/2015/06/21/deriving-incomplete-type-class-instances/
[circe-codec]: https://github.com/travisbrown/circe/issues/133
[metarest]: https://github.com/pathikrit/metarest
[monocle]: https://github.com/julien-truffaut/Monocle
[no-reflection]: https://gitter.im/travisbrown/circe?at=566effd73078c074765121ca
[on-reflection]: http://stackoverflow.com/a/33580411/334519
[slip-28]: https://github.com/scala/slip/pull/28
[tmtowtdi]: https://en.wikipedia.org/wiki/There%27s_more_than_one_way_to_do_it
[tpolecat]: https://twitter.com/tpolecat
