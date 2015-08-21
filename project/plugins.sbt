resolvers ++= Seq(
  Classpaths.typesafeReleases,
  Classpaths.sbtPluginReleases,
  "jgit-repo" at "http://download.eclipse.org/jgit/maven"
)

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.3")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.0")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")
addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.3")
addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.4")
addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "0.8.1")
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.4")
addSbtPlugin("org.scalastyle" %% "scalastyle-sbt-plugin" % "0.6.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.2.0")
addSbtPlugin("org.brianmckenna" % "sbt-wartremover" % "0.13")
addSbtPlugin("pl.project13.scala" % "sbt-jmh" % "0.2.2")
