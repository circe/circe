## Quick start

circe is published to [Maven Central][maven-central] and cross-built for Scala 2.10, 2.11, and 2.12,
so you can just add the following to your build:

```scala
val circeVersion = "0.8.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)
```

If you're using circe's generic derivation with Scala 2.10, or `@JsonCodec` the macro annotation
(with any Scala version), you'll also need to include the [Macro Paradise][paradise] compiler
plugin in your build:

```scala
addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full
)
```

Then type `sbt console` to start a REPL and then paste the following (this will also work from the
root directory of this repository):

```scala
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
// <console>:12: warning: Unused import
//        import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
//                        ^
// <console>:12: warning: Unused import
//        import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
//                                                 ^
// <console>:12: warning: Unused import
//        import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
//                                                                    ^
// <console>:12: warning: Unused import
//        import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._
//                                                                                       ^
// import io.circe._
// import io.circe.generic.auto._
// import io.circe.parser._
// import io.circe.syntax._

sealed trait Foo
// <console>:10: warning: Unused import
//        import io.circe._
//                        ^
// <console>:13: warning: Unused import
//        import io.circe.generic.auto._
//                                     ^
// <console>:16: warning: Unused import
//        import io.circe.parser._
//                               ^
// <console>:19: warning: Unused import
//        import io.circe.syntax._
//                               ^
// defined trait Foo

case class Bar(xs: List[String]) extends Foo
// <console>:10: warning: Unused import
//        import io.circe._
//                        ^
// <console>:13: warning: Unused import
//        import io.circe.generic.auto._
//                                     ^
// <console>:16: warning: Unused import
//        import io.circe.parser._
//                               ^
// <console>:19: warning: Unused import
//        import io.circe.syntax._
//                               ^
// defined class Bar

case class Qux(i: Int, d: Option[Double]) extends Foo
// <console>:10: warning: Unused import
//        import io.circe._
//                        ^
// <console>:13: warning: Unused import
//        import io.circe.generic.auto._
//                                     ^
// <console>:16: warning: Unused import
//        import io.circe.parser._
//                               ^
// <console>:19: warning: Unused import
//        import io.circe.syntax._
//                               ^
// defined class Qux

val foo: Foo = Qux(13, Some(14.0))
// <console>:10: warning: Unused import
//        import io.circe._
//                        ^
// <console>:13: warning: Unused import
//        import io.circe.generic.auto._
//                                     ^
// <console>:16: warning: Unused import
//        import io.circe.parser._
//                               ^
// <console>:19: warning: Unused import
//        import io.circe.syntax._
//                               ^
// foo: Foo = Qux(13,Some(14.0))

foo.asJson.noSpaces
// <console>:10: warning: Unused import
//        import io.circe._
//                        ^
// <console>:16: warning: Unused import
//        import io.circe.parser._
//                               ^
// res0: String = {"Qux":{"i":13,"d":14.0}}

decode[Foo](foo.asJson.spaces4)
// <console>:10: warning: Unused import
//        import io.circe._
//                        ^
// res1: Either[io.circe.Error,Foo] = Right(Qux(13,Some(14.0)))
```

No boilerplate, no runtime reflection.
