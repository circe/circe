scalaVersion := "2.11.8"

scalacOptions += "-deprecation"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-generic" % "0.4.1",
  "io.circe" %% "circe-streaming" % "0.4.1",
  "io.iteratee" %% "iteratee-task" % "0.3.1"
)
