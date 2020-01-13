scalaVersion := "2.13.1"

scalacOptions += "-deprecation"

libraryDependencies ++= Seq(
  "io.circe"    %% "circe-generic"  % "0.13.0-M2",
  "io.circe"    %% "circe-iteratee" % "0.13.0-M2",
  "io.iteratee" %% "iteratee-files" % "0.19.0"
)
