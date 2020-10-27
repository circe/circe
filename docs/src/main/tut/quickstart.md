## Quick start

circe is published to [Maven Central][maven-central] and cross-built for Scala 2.12 and 2.13,
so you can just add the following to your build:

```scala
val circeVersion = "0.13.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)
```

In case of large or deep-nested case classes, there is a chance to get stack overflow during compilation,
please refer to [known-issues](codecs/known-issues.html) for workaround.

If you're using circe-generic-extra's `@JsonCodec` macro annotations,
you'll need to add `-Ymacro-annotations` to your compiler options on Scala 2.13,
or to include the [Macro Paradise][paradise] compiler plugin in your build on
earlier Scala versions:

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

And here's a more elaborate example that uses some of Circe's features you may want to use when encoding and decoding real world (read: inconsistent) JSON payloads:


```scala
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.generic.extras._

implicit val config: Configuration = Configuration.default
  .copy(transformMemberNames = (x) => x.capitalize)

@ConfiguredJsonCodec
case class Label(name: String, color: String)

@ConfiguredJsonCodec
case class Issue(
    id: Int,
    number: Int,
    state: String,
    title: String,
    body: String,
    @JsonKey("important_comments") comments: Int,
    labels: List[Label]
)

val json = """{ "Id": 1,
  "Number": 1234,
  "State": "open",
  "Title": "Found a bug",
  "Body": "Something's really broken",
  "important_comments": 5,
  "Labels": [
    { "Name": "Urgent", "Color": "#ff0000" },
    { "Name": "Frontend", "Color": "#ffffff" }
  ]
}"""

val decoded = decode[Issue](json)
println(decoded)
// Right(
//   Issue(
//     1, 1234, open, Found a bug,Something's really broken, 5, List(Label(Urgent,#ff0000), Label(Frontend,#ffffff))
//   )
// ): Either[io.circe.Error,Issue]

println(decoded.right.get.asJson.toString)
// {
//   "Id" : 1,
//   "Number" : 1234,
//   "State" : "open",
//   "Title" : "Found a bug",
//   "Body" : "Something's really broken",
//   "important_comments" : 5,
//   "Labels" : [
//     {
//       "Name" : "Urgent",
//       "Color" : "#ff0000"
//     },
//     {
//       "Name" : "Frontend",
//       "Color" : "#ffffff"
//     }
//   ]
// }: String
```
