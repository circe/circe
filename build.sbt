import sbtunidoc.Plugin.UnidocKeys._
import ReleaseTransformations._

lazy val buildSettings = Seq(
  organization := "io.circe",
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
  scalacOptions in (Compile, console) := compilerOptions,
  libraryDependencies ++= Seq(
    compilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
  ) ++ testDependencies.map(_ % "test"),
  resolvers += Resolver.sonatypeRepo("snapshots")
)

lazy val allSettings = buildSettings ++ baseSettings ++ unidocSettings ++ publishSettings

lazy val docSettings = site.settings ++ ghpages.settings ++ unidocSettings ++ Seq(
  site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "api"),
  scalacOptions in (ScalaUnidoc, unidoc) ++= Seq("-groups", "-implicits"),
  git.remoteRepo := "git@github.com:travisbrown/circe.git",
  unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(benchmark)
)

lazy val root = project.in(file("."))
  .settings(allSettings)
  .settings(docSettings)
  .settings(noPublishSettings)
  .settings(
    initialCommands in console :=
      """
        |import io.circe._
        |import io.circe.generic.auto._
        |import io.circe.jawn._
        |import io.circe.syntax._
        |import cats.data.Xor
      """.stripMargin
  )
  .aggregate(core, cats, tests, generic, jawn, async, benchmark)
  .dependsOn(core, cats, generic, jawn, async)

lazy val core = project
  .settings(moduleName := "circe-core")
  .settings(allSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.spire-math" %% "cats-core" % "0.1.2",
      "org.spire-math" %% "cats-std" % "0.1.2",
      "io.argonaut" %% "argonaut" % "6.1" % "test"
    )
  )

lazy val cats = project
  .settings(moduleName := "circe-cats")
  .settings(allSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.spire-math" %% "cats-core" % "0.1.2",
      "org.spire-math" %% "cats-std" % "0.1.2",
      "io.argonaut" %% "argonaut" % "6.1" % "test"
    )
  )
  .dependsOn(core)

lazy val tests = project
  .settings(moduleName := "circe-tests")
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    libraryDependencies ++= Seq(
      "io.argonaut" %% "argonaut" % "6.1" % "test"
    )
  )
  .dependsOn(core, core % "test->test", cats)

lazy val generic = project
  .settings(moduleName := "circe-generic")
  .settings(allSettings)
  .settings(
    libraryDependencies += "com.chuusai" %% "shapeless" % "2.2.5"
  )
  .dependsOn(core, tests % "test->test")

lazy val jawn = project
  .settings(moduleName := "circe-jawn")
  .settings(allSettings)
  .settings(
    libraryDependencies += "org.spire-math" %% "jawn-parser" % "0.8.0"
  )
  .dependsOn(core, tests % "test->test")

lazy val async = project
  .settings(moduleName := "circe-async")
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    libraryDependencies += "com.twitter" %% "util-core" % "6.26.0"
  )
  .dependsOn(core, jawn)

lazy val benchmark = project
  .settings(moduleName := "circe-benchmark")
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    libraryDependencies += "io.argonaut" %% "argonaut" % "6.1"
  )
  .enablePlugins(JmhPlugin)
  .dependsOn(core, generic, jawn)

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  homepage := Some(url("https://github.com/travisbrown/circe")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  autoAPIMappings := true,
  apiURL := Some(url("https://travisbrown.github.io/circe/api/")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/travisbrown/circe"),
      "scm:git:git@github.com:travisbrown/circe.git"
    )
  ),
  pomExtra := (
    <developers>
      <developer>
        <id>travisbrown</id>
        <name>Travis Brown</name>
        <url>https://twitter.com/travisbrown</url>
      </developer>
    </developers>
  )
)

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val sharedReleaseProcess = Seq(
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    runTest,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    publishArtifacts,
    setNextVersion,
    commitNextVersion,
    ReleaseStep(action = Command.process("sonatypeReleaseAll", _)),
    pushChanges
  )
)

credentials ++= (
  for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    username,
    password
  )
).toSeq
