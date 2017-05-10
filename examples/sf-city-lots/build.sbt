scalaVersion := "2.12.2"

scalacOptions += "-deprecation"

val circeVersion = "0.8.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-streaming" % circeVersion,
  "io.iteratee" %% "iteratee-scalaz" % "0.11.0"
)
