scalaVersion := "2.12.4"

scalacOptions += "-deprecation"

val circeVersion = "0.9.0"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-iteratee" % circeVersion,
  "io.iteratee" %% "iteratee-monix" % "0.17.0"
)
