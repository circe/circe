# Design guide and justification

In this document I'll attempt to explain some of the design decisions that I've made in circe, and
to justify some of the gaps in the API where circe doesn't (currently) support the same kinds of
functionality you'll find in other Scala JSON libraries. I'm hoping to close all of these gaps, but
in most cases they exist because I've put constraints on the project that I don't want to relax
until I've experimented with other solutions.

Please don't read this as a general Scala style guide—for one thing it's entirely about writing
library code and designing library APIs, and some of it is JSON-specific. It's also based on lots of
more-or-less well-considered personal preferences that are subject to change.

1. **Your model code shouldn't (have to) know anything about serialization.**
2. **You generally shouldn't need or want to work with JSON ASTs directly.**
3. **Implicit scope should not be used for configuration.**
4. **The public API should not contain unnecessary methods.**
5. **The public API should not contain unnecessary types.**
6. **Not all ADTs should expose their constructors.**
7. **The library implementation does not model usage.**

[metarest]: https://github.com/pathikrit/metarest
[slip-28]: https://github.com/scala/slip/pull/28
