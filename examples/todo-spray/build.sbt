name := "TodoService"
version := "0.1.0"
scalaVersion := "2.11.12"

val sprayVersion = "1.3.4"
val circeVersion = "0.11.1"
val circeSprayVersion = "0.11.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-java8" % circeVersion,
  "io.circe" %% "circe-spray" % circeSprayVersion,
  "io.spray" %% "spray-can" % sprayVersion,
  "io.spray" %% "spray-routing-shapeless23" % sprayVersion
)
