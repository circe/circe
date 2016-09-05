## Quick start

circe is published to [Maven Central][maven-central] and cross-built for Scala 2.10 and 2.11, so
you can just add the following to your build:

```scala
val circeVersion = "0.5.1"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)
```

If you are using circe's generic derivation (with Scala 2.10), or the macro annotation `@JsonCodec` (with
Scala 2.10 or Scala 2.11), you'll also need to include the [Macro Paradise][paradise] compiler
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
// import io.circe._
// import io.circe.generic.auto._
// import io.circe.parser._
// import io.circe.syntax._

sealed trait Foo
// defined trait Foo

case class Bar(xs: List[String]) extends Foo
// defined class Bar

case class Qux(i: Int, d: Option[Double]) extends Foo
// defined class Qux

val foo: Foo = Qux(13, Some(14.0))
// foo: Foo = Qux(13,Some(14.0))

foo.asJson.noSpaces
// res0: String = {"Qux":{"i":13,"d":14.0}}

decode[Foo](foo.asJson.spaces4)
// res1: cats.data.Xor[io.circe.Error,Foo] = Right(Qux(13,Some(14.0)))
```

No boilerplate, no runtime reflection.
