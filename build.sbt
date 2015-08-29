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

val catsVersion = "0.1.3-SNAPSHOT"

lazy val testSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scalacheck" %%% "scalacheck" % "1.12.5-SNAPSHOT" % "test",
    "org.scalatest" %%% "scalatest" % "3.0.0-M7" % "test",
    "org.spire-math" %%% "cats-laws" % catsVersion % "test",
    "org.typelevel" %%% "discipline" % "0.4" % "test"
  )
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
    "org.typelevel" %% "export-hook" % "1.0.1-SNAPSHOT",
    compilerPlugin("org.scalamacros" % "paradise" % "2.1.0-M5" cross CrossVersion.full)
  ),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  ScoverageSbtPlugin.ScoverageKeys.coverageHighlighting := (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 10)) => false
      case _ => true
    }
  )
)

lazy val allSettings = buildSettings ++ baseSettings ++ testSettings ++ publishSettings

lazy val commonJsSettings = Seq(
  postLinkJSEnv := NodeJSEnv().value,
  scalaJSStage in Global := FastOptStage
)

lazy val docSettings = site.settings ++ ghpages.settings ++ unidocSettings ++ Seq(
  site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "api"),
  scalacOptions in (ScalaUnidoc, unidoc) ++= Seq("-groups", "-implicits"),
  git.remoteRepo := "git@github.com:travisbrown/circe.git",
  unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(benchmark, coreJS, genericJS)
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
  .aggregate(coreJVM, coreJS, genericJVM, genericJS, jawn, async, benchmark)
  .dependsOn(coreJVM, genericJVM, jawn, async)

lazy val core = crossProject
  .settings(moduleName := "circe-core")
  .settings(allSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "org.spire-math" %%% "cats-core" % catsVersion
    ),
    sourceGenerators in Compile <+= (sourceManaged in Compile).map(Boilerplate.gen)
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "io.argonaut" %% "argonaut" % "6.1" % "test"
    )
  )
  .jsSettings(commonJsSettings: _*)

lazy val coreJVM = core.jvm
lazy val coreJS = core.js

lazy val generic = crossProject
  .settings(moduleName := "circe-generic")
  .settings(allSettings: _*)
  .settings(
    libraryDependencies += "com.chuusai" %%% "shapeless" % "2.3.0-SNAPSHOT"
  )
  .jsSettings(commonJsSettings: _*)
  .dependsOn(core, core % "test->test")

lazy val genericJVM = generic.jvm
lazy val genericJS = generic.js

lazy val jawn = project
  .settings(moduleName := "circe-jawn")
  .settings(allSettings)
  .settings(
    libraryDependencies += "org.spire-math" %% "jawn-parser" % "0.8.3"
  )
  .dependsOn(coreJVM, coreJVM % "test->test")

lazy val async = project
  .settings(moduleName := "circe-async")
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    libraryDependencies += "com.twitter" %% "util-core" % "6.27.0"
  )
  .dependsOn(coreJVM, jawn)

lazy val benchmark = project
  .settings(moduleName := "circe-benchmark")
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    libraryDependencies += "io.argonaut" %% "argonaut" % "6.1"
  )
  .enablePlugins(JmhPlugin)
  .dependsOn(coreJVM, genericJVM, jawn)

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

val jvmProjects = Seq(
  "coreJVM",
  "genericJVM",
  "jawn",
  "async",
  "benchmark"
)

val jsProjects = Seq(
  "coreJS",
  "genericJS"
)

addCommandAlias("buildJVM", jvmProjects.map(";" + _ + "/test:compile").mkString)
addCommandAlias("validateJVM", ";buildJVM" + jvmProjects.map(";" + _ + "/test").mkString + ";scalastyle")
addCommandAlias("buildJS", jsProjects.map(";" + _ + "/test:compile").mkString)
addCommandAlias("validateJS", ";buildJS" + jsProjects.map(";" + _ + "/test").mkString + ";scalastyle")
addCommandAlias("validate", ";validateJVM;validateJS")
