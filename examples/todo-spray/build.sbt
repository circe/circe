name := "TodoService"
version := "0.1.0"
scalaVersion := "2.11.8"

val sprayVersion = "1.3.3"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-generic"             % "0.5.0",
  "io.circe" %% "circe-java8"               % "0.5.0",
  "io.circe" %% "circe-spray"               % "0.5.0",
  "io.spray" %% "spray-can"                 % sprayVersion,
  "io.spray" %% "spray-routing-shapeless23" % sprayVersion
)
