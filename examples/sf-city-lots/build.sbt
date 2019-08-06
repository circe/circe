scalaVersion := "2.12.9"

scalacOptions += "-deprecation"

libraryDependencies ++= Seq(
  "io.circe"    %% "circe-generic"  % "0.11.1",
  "io.circe"    %% "circe-iteratee" % "0.12.0",
  "io.iteratee" %% "iteratee-files" % "0.18.0"
)
