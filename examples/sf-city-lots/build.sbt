scalaVersion := "2.12.5"

scalacOptions += "-deprecation"

val circeVersion = "0.9.2"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-iteratee" % circeVersion,
  "io.iteratee" %% "iteratee-monix" % "0.17.0"
)
