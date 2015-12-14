# Design guide and justification

In this document I'll attempt to explain some of the design decisions that I've made in circe, and
to justify some of the gaps in the API where circe doesn't (currently) support the same kinds of
functionality you'll find in other Scala JSON libraries. I'm hoping to close all of these gaps, but
in most cases they exist because I've put constraints on the project that I don't want to relax
until I've experimented with other solutions.

Please don't read this as a general Scala style guide—for one thing it's entirely about writing
library code and designing a library API, and some of it is JSON-specific. It's also based on lots
of more-or-less well-considered personal preferences that are subject to change.

1. **Your model code shouldn't (have to) know anything about serialization.**
    Sometimes it's appropriate to have serialization code in your model, and sometimes it isn't. As
    a serialization library, circe should not penalize the cases where it's not appropriate, and the
    same functionality should be available in both situations, with no more than a single extra
    import being required in the latter.

    **What this means concretely.** [MetaRest][metarest] is a very interesting project, and many
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

    **What this means concretely.** If something like SLIP-28 ever actually lands in the standard
    library, I'll probably adopt it (assuming it doesn't violate other design principles here—which
    is a huge assumption), but I hope most users won't even notice that change.

3. **Implicit scope should not be used for configuration.**
    Lots of people have asked for a way to configure generic codec derivation to use e.g. a `type`
    field as the discriminator for sealed trait hierarchies, or to use snake case for member names.
    [argonaut-shapeless][argonaut-shapeless] supports this quite straightforwardly with a
    [`JsonCoproductCodec`][argonaut-shapeless-7] type that the user can provide implicitly.

    I don't
    want to criticize this approach—it's entirely idiomatic Scala, and it often works well in
    practice—but I personally don't like using implicit values for configuration, and I'd like to
    avoid it in circe until I am 100% convinced that there's no alternative way to provide this
    functionality.

    **What this means concretely.** You'll probably never see an implicit argument that isn't a type
    class instance—i.e. that isn't a type constructor applied to a type in your model—in circe, and
    configuration of generic codec derivation is going to be relatively limited (compared to e.g.
    argonaut-shapeless) until we find a nice way to do this kind of thing with type tags or
    something similar.

4. **The public API should not contain unnecessary methods.**
    I'm all for [TMTOWTDI][tmtowtdi] at certain levels of abstraction, but I think redundancy in
    library API design is a terrible way to treat users (especially new ones).

    **What this means concretely.** If the standard way to create a successful `ACursor` from an
    `HCursor` is `ACursor.ok`, there should not be an equivalent `toACursor` method on `HCursor`.
    Don't even get me started on symbolic operator aliases.

5. **The public API should not contain unnecessary types.**
    Argonaut (like many other great serialization libraries) includes a `Codec` type class that
    combines `Encoder` and `Decoder`, but it's only really useful for definitions: it allows you to
    define an encoder-decoder pair together, but its generally not useful as a type constraint,
    since many types with both `Encoder` and `Decoder` instances will not have a `Codec` instance.
    I personally find this confusing, and would prefer to keep convenience-oriented types like this
    out of the API.

    **What this means concretely.** There's unlikely to be a `Codec` in circe until we come up with
    a [clean way][circe-codec] to make it useful as a type constraint (although if demand gets loud
    enough I would be willing to consider compromise here).

6. **Not all ADTs should expose their constructors.** Coming soon.
7. **The library implementation does not model usage.** Coming soon.

[argonaut-shapeless]: https://github.com/alexarchambault/argonaut-shapeless
[argonaut-shapeless-7]: https://github.com/alexarchambault/argonaut-shapeless/pull/7
[incompletes]: https://meta.plasm.us/posts/2015/06/21/deriving-incomplete-type-class-instances/
[circe-codec]: https://github.com/travisbrown/circe/issues/133
[metarest]: https://github.com/pathikrit/metarest
[monocle]: https://github.com/julien-truffaut/Monocle
[slip-28]: https://github.com/scala/slip/pull/28
[tmtowtdi]: https://en.wikipedia.org/wiki/There%27s_more_than_one_way_to_do_it
