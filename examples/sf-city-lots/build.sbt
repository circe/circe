scalaVersion := "2.11.7"

resolvers += Resolver.sonatypeRepo("snapshots")

scalacOptions += "-deprecation"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-generic" % "0.3.0-SNAPSHOT",
  "io.circe" %% "circe-streaming" % "0.3.0-SNAPSHOT",
  "io.iteratee" %% "iteratee-task" % "0.2.0"
)
