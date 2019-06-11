# Processing large JSON files with circe

This example demonstrates how to use [iteratee.io][iteratee-io] and
[circe-iteratee][circe-iteratee] to process JSON files that may not fit in memory.

## Getting started

This example uses a JSON representation (prepared by [Mirco Zeiss][zemirco]) of a shapefile
distributed by [SF OpenData][sf-opendata]. The [file](https://github.com/zeMirco/sf-city-lots-json)
is 189 MB and is not included here, so you'll have to download it before running the code in the
example:

```bash
wget https://raw.githubusercontent.com/zemirco/sf-city-lots-json/master/citylots.json
```

To keep things simple we'll remove the top-level object from the JSON so that the file contains a
large JSON array (which is likely to be what we'll see in a context where we need to parse streaming
JSON, anyway):

```bash
awk 'BEGIN{print"["} NR>4 {print l} {l=$0}' citylots.json > data.json
```

(You can also do this by hand, of course.)

## Decoders

Next we'll define our decoders for the JSON representation of the shapefile. We'll start with our
imports, which bring the `Decoder` name into scope and introduce the implicits that will
[generically derive][generic-derivation] decoder instances for any case classes we don't explicitly
provide instances for.

All of the Scala code from here on can be typed into a REPL, which you can open in this directory
with `sbt console`. For case classes with companion objects, you'll need to use `:paste` to have the
definitions evaluated together.

```scala
import io.circe.Decoder, io.circe.generic.auto._
```

If we were on Scala 2.11, we'd also need to import `cats.syntax.either._` here since we're
using `map` and `flatMap` on `Either` values below. This is no longer necessary since Scala 2.12, which
introduced right-biasing for `Either`.

We'll start with a type representing a coordinate pair. We want this decoder to recognize either of
the following formats:

```json
[1.0, 2.0]
[1.0, 2.0, 0.0]
```

To do this we use the `or` combinator, which will first try to decode a tuple of numbers and then a
triple:

```scala
case class Coord(x: Double, y: Double)

object Coord {
  implicit val decodeCoord: Decoder[Coord] =
    Decoder[(Double, Double)].map(p => Coord(p._1, p._2)).or(
      Decoder[(Double, Double, Double)].map(p => Coord(p._1, p._2))
    )
}
```

Next we'll define a `Geometry` type that represents a shape. This dataset includes two kinds of
polygons with different representations, so we'll use an ADT with two case classes:

```scala
sealed trait Geometry
case class Polygon(coordinates: List[List[Coord]]) extends Geometry
case class MultiPolygon(coordinates: List[List[List[Coord]]]) extends Geometry

object Geometry {
  implicit val decodeGeometry: Decoder[Geometry] = Decoder.instance(c =>
    c.downField("type").as[String].flatMap {
      case "Polygon" => c.as[Polygon]
      case "MultiPolygon" => c.as[MultiPolygon]
    }
  )
}
```

Note that we don't explicitly give decoder instances for `Polygon` or `MultiPolygon`—these are very
simple types, and generic derivation will work for them. If we wanted to avoid generic derivation
entirely, we could skip the `io.circe.generic.auto._` import above and write instances for these two
case classes by hand:

```scala
object Polygon {
  implicit val decodePolygon: Decoder[Polygon] =
    Decoder[List[List[Coord]]].prepare(
      _.downField("coordinates")
    ).map(Polygon(_))
}

object MultiPolygon {
  implicit val decodeMultiPolygon: Decoder[MultiPolygon] =
    Decoder[List[List[List[Coord]]]].prepare(
      _.downField("coordinates")
    ).map(MultiPolygon(_))
}
```

Which approach you should prefer is a matter of taste and the details of your use case, but in
general using generically derived instances when possible will result in less boilerplate and more
maintainable code.

We also can't use generic derivation for `Geometry`, since by default the derived decoder expects an
enclosing object where the key name determines the subtype, like this:

```json
{
  "Polygon": {
    "coordinates": [[[1.0, 2.0, 0.0]]]
  }
}
```

Unfortunately the JSON we actually have looks like this:

```json
{
  "type": "Polygon",
  "coordinates": [[[1.0, 2.0, 0.0]]]
}
```

Defining our decoder for `Geometry` isn't too bad, though.

Finally we can write our decoder for the top-level type representing a city lot:

```scala
case class Lot(tpe: String, props: Map[String, String], geo: Option[Geometry])

object Lot {
  implicit val decodeLot: Decoder[Lot] = Decoder.instance(c =>
    for {
      t <- c.downField("type").as[String]
      p <- c.downField("properties").as[Map[String, Option[String]]]
      g <- c.downField("geometry").as[Option[Geometry]]
    } yield Lot(t, p.collect { case (k, Some(v)) => (k, v) }, g)
  )
}
```

In real code we'd probably want to use the `|@|` syntax provided by [cats][cats] instead of the
`for`-comprehension in order to be able to [accumulate errors][error-accumulation] when needed, but
in order to keep things simple I've gone with the `for`-comprehension.

## Streaming

circe-iteratee provides generic enumeratees for parsing and decoding that work over any type
constructor with a `MonadError` instance. We'll use cats-effect's `IO` since we need to read from a
file.

```scala
import cats.effect.IO
import io.circe.iteratee.{ byteArrayParser, decoder }
import io.iteratee.{ Enumeratee, Iteratee }
import io.iteratee.files.readBytes
import java.io.File
```

And then we can define an `Enumerator` that will let us read lines from the file, parse them
asynchronously with jawn, and decode them into `Task` values:

```scala
val lots =
  readBytes(new File("data.json")).through(byteArrayParser).through(decoder[Task, Lot])
```

This line doesn't do any real work—it doesn't even open the file. It just represents a source of
lots that we can process with an iteratee. For example, we can count the number of lots with
`length`:

```scala
scala> val countLots = lots.into(Iteratee.length)
countLots: cats.effect.IO[Long] = IO$1659916113

scala> countLots.unsafeRunSync
res0: Long = 206560
```

Or we can count how many of each geometry type there are:

```scala
scala> import cats.instances.int._, cats.instances.map._
import cats.instances.int._
import cats.instances.map._

scala> val countTypes: IO[Map[String, Int]] = lots.through(
     |   Enumeratee.collect {
     |     case Lot(_, _, Some(Polygon(_))) => Map("Polygon" -> 1)
     |     case Lot(_, _, Some(MultiPolygon(_))) => Map("MultiPolygon" -> 1)
     |   }
     | ).into(Iteratee.sum)
countTypes: cats.effect.IO[Map[String,Int]] = IO$2090607373

scala> countTypes.unsafeRunSync
res1: Map[String,Int] = Map(MultiPolygon -> 120, Polygon -> 206434)
```

Or simply gather the first few lots into a sequence:

```scala
val first3 = lots.into(Iteratee.take(3)).unsafeRunSync
first3: Vector[Lot] = Vector(Lot(Feature,Map(MAPBLKLOT -> 0001001, ...
```

Because we're working with iteratees, we don't need to worry about managing resources manually—in
each of these cases the file will be closed when the processing is done (even if we only read at a
few lines from the file, or if we run into decoding or I/O errors during the processing).

[cats]: https://github.com/typelevel/cats
[cats-32]: https://github.com/typelevel/cats/issues/32
[circe-iteratee]: https://github.com/circe/circe-iteratee
[error-accumulation]: https://meta.plasm.us/posts/2015/12/17/error-accumulating-decoders-in-circe/
[jawn]: https://github.com/non/jawn
[generic-derivation]: https://meta.plasm.us/posts/2015/11/08/type-classes-and-generic-derivation/
[iteratee-io]: https://github.com/travisbrown/iteratee
[sf-opendata]: https://data.sfgov.org/
[zemirco]: https://github.com/zemirco
