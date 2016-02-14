scalaVersion := "2.11.7"

scalacOptions += "-deprecation"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-generic" % "0.3.0",
  "io.circe" %% "circe-streaming" % "0.3.0",
  "io.iteratee" %% "iteratee-task" % "0.2.1"
)
