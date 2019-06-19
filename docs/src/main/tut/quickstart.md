## Quick start

circe is published to [Maven Central][maven-central] and cross-built for Scala 2.12 and 2.13,
so you can just add the following to your build:

```scala
val circeVersion = "0.11.1"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)
```

If you're using circe-generic-extra's `@JsonCodec` macro annotation (with any Scala version before 2.13),
you'll also need to include the [Macro Paradise][paradise] compiler plugin in your build:

```scala
addCompilerPlugin(
  "org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full
)
```

Then type `sbt console` to start a REPL and then paste the following (this will also work from the
root directory of this repository):

{% scalafiddle %}
```scala
import io.circe._, io.circe.generic.auto._, io.circe.parser._, io.circe.syntax._

sealed trait Foo
case class Bar(xs: Vector[String]) extends Foo
case class Qux(i: Int, d: Option[Double]) extends Foo

val foo: Foo = Qux(13, Some(14.0))

val json = foo.asJson.noSpaces
println(json)

val decodedFoo = decode[Foo](json)
println(decodedFoo)
```
{% endscalafiddle %}

Alternatively you can experiment with Circe directly in your browser by clicking the `Run` button in the code block and
making modifications in the code.

No boilerplate, no runtime reflection.
