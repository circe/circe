scalaVersion := "2.12.1"

scalacOptions += "-deprecation"

val circeVersion = "0.7.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-streaming" % circeVersion,
  "io.iteratee" %% "iteratee-scalaz" % "0.9.0"
)
