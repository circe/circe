name := "TodoService"
version := "0.1.0"
scalaVersion := "2.11.8"

val sprayVersion = "1.3.3"
val circeVersion = "0.6.0-RC1"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-generic"             % circeVersion,
  "io.circe" %% "circe-java8"               % circeVersion,
  "io.circe" %% "circe-spray"               % circeVersion,
  "io.spray" %% "spray-can"                 % sprayVersion,
  "io.spray" %% "spray-routing-shapeless23" % sprayVersion
)
