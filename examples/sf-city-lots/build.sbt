scalaVersion := "2.11.8"

scalacOptions += "-deprecation"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-generic" % "0.5.1",
  "io.circe" %% "circe-streaming" % "0.5.1",
  "io.iteratee" %% "iteratee-scalaz" % "0.6.1"
)
