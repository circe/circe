scalaVersion := "2.11.8"

scalacOptions += "-deprecation"

val circeVersion = "0.5.4"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-streaming" % circeVersion,
  "io.iteratee" %% "iteratee-scalaz" % "0.6.1"
)
