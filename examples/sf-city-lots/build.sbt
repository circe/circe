scalaVersion := "2.12.0-RC2"

scalacOptions += "-deprecation"

val circeVersion = "0.6.0-RC1"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-streaming" % circeVersion,
  "io.iteratee" %% "iteratee-scalaz" % "0.7.0"
)
