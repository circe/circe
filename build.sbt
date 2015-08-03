import sbtunidoc.Plugin.UnidocKeys._

lazy val buildSettings = Seq(
  organization := "io.jfc",
  version := "0.1.0-SNAPSHOT",
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.10.5", "2.11.7")
)

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-unchecked",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture"
)

lazy val testDependencies = Seq(
  "org.scalacheck" %% "scalacheck" % "1.12.5-SNAPSHOT",
  "org.scalatest" %% "scalatest" % "2.2.5",
  "org.spire-math" %% "cats-laws" % "0.1.2",
  "org.typelevel" %% "discipline" % "0.3"
)

lazy val baseSettings = Seq(
  scalacOptions ++= compilerOptions ++ (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => Seq("-Ywarn-unused-import")
      case _ => Nil
    }
  ),
  libraryDependencies ++= Seq(
    compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
  ) ++ testDependencies.map(_ % "test"),
  resolvers += Resolver.sonatypeRepo("snapshots")
  /** Too much of a pain for now.
  wartremoverWarnings in (Compile, compile) ++= Warts.allBut(
    Wart.NoNeedForMonad
  ),
  wartremoverExcluded +=
    baseDirectory.value / "src" / "main" / "scala" / "io" / "jfc" / "Printer.scala"
  */
)

lazy val allSettings = buildSettings ++ baseSettings ++ unidocSettings ++ publishSettings

lazy val docSettings = site.settings ++ ghpages.settings ++ unidocSettings ++ Seq(
  site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "api"),
  scalacOptions in (ScalaUnidoc, unidoc) := Seq("-groups", "-implicits"),
  git.remoteRepo := "git@github.com:travisbrown/jfc.git",
  unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(benchmark)
)

lazy val root = project.in(file("."))
  .settings(allSettings ++ noPublish)
  .settings(docSettings)
  .settings(noPublish)
  .settings(scalacOptions in (Compile, console) := compilerOptions)
  .settings(
    initialCommands in console :=
      """
        |import io.jfc._
        |import io.jfc.auto._
        |import io.jfc.jawn._
        |import io.jfc.syntax._
        |import cats.data.Xor
      """.stripMargin
  )
  .aggregate(core, auto, jawn, async, benchmark)
  .dependsOn(core, auto, jawn, async)

lazy val core = project
  .settings(moduleName := "jfc-core")
  .settings(allSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.spire-math" %% "cats-core" % "0.1.2",
      "org.spire-math" %% "cats-std" % "0.1.2"
    )
  )

lazy val auto = project
  .settings(moduleName := "jfc-auto")
  .settings(allSettings)
  .settings(
    libraryDependencies += "com.chuusai" %% "shapeless" % "2.2.5"
  )
  .dependsOn(core, core % "test->test")

lazy val jawn = project
  .settings(moduleName := "jfc-jawn")
  .settings(allSettings)
  .settings(
    libraryDependencies += "org.spire-math" %% "jawn-parser" % "0.8.0"
  )
  .dependsOn(core, core % "test->test")

lazy val async = project
  .settings(moduleName := "jfc-async")
  .settings(allSettings)
  .settings(
    libraryDependencies += "com.twitter" %% "util-core" % "6.26.0"
  )
  .dependsOn(core, jawn)

lazy val benchmark = project
  .settings(moduleName := "jfc-benchmark")
  .settings(allSettings)
  .settings(noPublish)
  .settings(
    libraryDependencies += "io.argonaut" %% "argonaut" % "6.1"
  )
  .enablePlugins(JmhPlugin)
  .dependsOn(core, auto, jawn)

lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact := true,
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  publishArtifact in Test := false,
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  homepage := Some(url("https://github.com/travisbrown/jfc")),
  autoAPIMappings := true,
  apiURL := Some(url("https://travisbrown.github.io/jfc/api/")),
  pomExtra := (
    <scm>
      <url>git://github.com/travisbrown/jfc.git</url>
      <connection>scm:git://github.com/travisbrown/jfc.git</connection>
    </scm>
    <developers>
      <developer>
        <id>travisbrown</id>
        <name>Travis Brown</name>
        <url>https://twitter.com/travisbrown</url>
      </developer>
    </developers>
  )
)

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {}
)
