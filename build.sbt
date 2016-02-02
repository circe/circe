import sbtunidoc.Plugin.UnidocKeys._
import ReleaseTransformations._

lazy val buildSettings = Seq(
  organization := "io.circe",
  scalaVersion := "2.11.7",
  crossScalaVersions := Seq("2.10.6", "2.11.7")
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

lazy val catsVersion = "0.4.0"
lazy val shapelessVersion = "2.3.0-SNAPSHOT"
lazy val refinedVersion = "0.3.3"

lazy val scalaTestVersion = "3.0.0-M9"
lazy val scalaCheckVersion = "1.12.5"
lazy val disciplineVersion = "0.4"

lazy val baseSettings = Seq(
  scalacOptions ++= compilerOptions ++ (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => Seq("-Ywarn-unused-import")
      case _ => Nil
    }
  ),
  scalacOptions in (Compile, console) := compilerOptions,
  scalacOptions in (Compile, test) := compilerOptions,
  libraryDependencies ++= Seq(
    "org.typelevel" %% "export-hook" % "1.1.0",
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
  ),
  (scalastyleSources in Compile) <++= unmanagedSourceDirectories in Compile,
  ivyConfigurations += config("compile-time").hide,
  unmanagedClasspath in Compile ++= update.value.select(configurationFilter("compile-time")),
  unmanagedClasspath in Test ++= update.value.select(configurationFilter("compile-time"))
)

lazy val allSettings = buildSettings ++ baseSettings ++ publishSettings

lazy val commonJsSettings = Seq(
  postLinkJSEnv := NodeJSEnv().value,
  scalaJSStage in Global := FastOptStage
)

lazy val docSettings = site.settings ++ ghpages.settings ++ unidocSettings ++ Seq(
  site.addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), "api"),
  scalacOptions in (ScalaUnidoc, unidoc) ++= Seq(
    "-groups",
    "-implicits",
    "-doc-source-url", scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
    "-sourcepath", baseDirectory.in(LocalRootProject).value.getAbsolutePath
  ),
  git.remoteRepo := "git@github.com:travisbrown/circe.git",
  unidocProjectFilter in (ScalaUnidoc, unidoc) :=
    inAnyProject -- inProjects(
      async,
      benchmark,
      coreJS,
      genericJS,
      java8,
      literal, literalJS,
      refinedJS,
      parseJS,
      tests,
      testsJS
    )
)

lazy val circe = project.in(file("."))
  .settings(allSettings)
  .settings(docSettings)
  .settings(noPublishSettings)
  .settings(
    initialCommands in console :=
      """
        |import io.circe._
        |import io.circe.generic.auto._
        |import io.circe.literal._
        |import io.circe.parse._
        |import io.circe.syntax._
        |import cats.data.Xor
      """.stripMargin
  )
  .aggregate(
    core, coreJS,
    generic, genericJS,
    literal, literalJS,
    refined, refinedJS,
    parse, parseJS,
    tests, testsJS,
    jawn,
    jackson,
    optics,
    async,
    benchmark
  )
  .dependsOn(core, generic, literal, parse)

lazy val coreBase = crossProject.in(file("core"))
  .settings(
    description := "circe core",
    moduleName := "circe-core",
    name := "core"
  )
  .settings(allSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % catsVersion
    ),
    sourceGenerators in Compile <+= (sourceManaged in Compile).map(Boilerplate.gen)
  )
  .jsSettings(commonJsSettings: _*)
  .jvmConfigure(_.copy(id = "core"))
  .jsConfigure(_.copy(id = "coreJS"))

lazy val core = coreBase.jvm
lazy val coreJS = coreBase.js

lazy val genericBase = crossProject.in(file("generic"))
  .settings(
    description := "circe generic",
    moduleName := "circe-generic",
    name := "generic"
  )
  .settings(allSettings: _*)
  .settings(
    libraryDependencies += "com.chuusai" %%% "shapeless" % shapelessVersion
  )
  .jsSettings(commonJsSettings: _*)
  .jvmConfigure(_.copy(id = "generic"))
  .jsConfigure(_.copy(id = "genericJS"))
  .dependsOn(coreBase)

lazy val generic = genericBase.jvm
lazy val genericJS = genericBase.js

lazy val literalBase = crossProject.crossType(CrossType.Pure).in(file("literal"))
  .settings(
    description := "circe literal",
    moduleName := "circe-literal",
    name := "literal"
  )
  .settings(allSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
      "org.typelevel" %%% "macro-compat" % "1.1.1-SNAPSHOT"
    ),
    publishArtifact in (Compile, packageDoc) := false,
    publishArtifact in packageDoc := false,
    sources in (Compile, doc) := Nil
  )
  .jsSettings(commonJsSettings: _*)
  .jvmConfigure(_.copy(id = "literal"))
  .jsConfigure(_.copy(id = "literalJS"))
  .dependsOn(coreBase)

lazy val literal = literalBase.jvm
lazy val literalJS = literalBase.js

lazy val refinedBase = crossProject.in(file("refined"))
  .settings(
    description := "circe refined",
    moduleName := "circe-refined",
    name := "refined"
  )
  .settings(allSettings: _*)
  .settings(
    libraryDependencies += "eu.timepit" %%% "refined" % refinedVersion
  )
  .jsSettings(commonJsSettings: _*)
  .jvmConfigure(_.copy(id = "refined"))
  .jsConfigure(_.copy(id = "refinedJS"))
  .dependsOn(coreBase)

lazy val refined = refinedBase.jvm
lazy val refinedJS = refinedBase.js

lazy val parseBase = crossProject.in(file("parse"))
  .settings(
    description := "circe parse",
    moduleName := "circe-parse",
    name := "parse"
  )
  .settings(allSettings: _*)
  .jsSettings(commonJsSettings: _*)
  .jvmConfigure(_.copy(id = "parse").dependsOn(jawn))
  .jsConfigure(_.copy(id = "parseJS"))
  .dependsOn(coreBase)

lazy val parse = parseBase.jvm
lazy val parseJS = parseBase.js

lazy val testsBase = crossProject.in(file("tests"))
  .settings(
    description := "circe tests",
    moduleName := "circe-tests",
    name := "tests"
  )
  .settings(allSettings: _*)
  .settings(noPublishSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.chuusai" %%% "shapeless" % shapelessVersion,
      "org.scalacheck" %%% "scalacheck" % scalaCheckVersion,
      "org.scalatest" %%% "scalatest" % scalaTestVersion,
      "org.typelevel" %%% "cats-laws" % catsVersion,
      "org.typelevel" %%% "discipline" % disciplineVersion,
      "eu.timepit" %%% "refined-scalacheck" % refinedVersion
    ),
    sourceGenerators in Test <+= (sourceManaged in Test).map(Boilerplate.genTests),
    unmanagedResourceDirectories in Compile +=
      file("tests") / "shared" / "src" / "main" / "resources"
  )
  .settings(
    ScoverageSbtPlugin.ScoverageKeys.coverageExcludedPackages := "io\\.circe\\.tests\\..*"
  )
  .jvmSettings(fork := true)
  .jsSettings(commonJsSettings: _*)
  .jvmConfigure(_.copy(id = "tests").dependsOn(jawn, jackson, async))
  .jsConfigure(
    _.copy(id = "testsJS").settings(
      libraryDependencies += "org.spire-math" %% "jawn-parser" % "0.8.3" % "compile-time"
    )
  )
  .dependsOn(coreBase, genericBase, literalBase, refinedBase, parseBase)

lazy val tests = testsBase.jvm
lazy val testsJS = testsBase.js

lazy val jawn = project
  .settings(
    description := "circe jawn",
    moduleName := "circe-jawn"
  )
  .settings(allSettings)
  .settings(
    libraryDependencies += "org.spire-math" %% "jawn-parser" % "0.8.3"
  )
  .dependsOn(core)

lazy val java8 = project
  .settings(
    description := "circe java8",
    moduleName := "circe-java8"
  )
  .settings(allSettings)
  .dependsOn(core, tests % "test")

lazy val streaming = project
  .settings(
    description := "circe streaming",
    moduleName := "circe-streaming"
  )
  .settings(allSettings)
  .settings(
    libraryDependencies += "io.iteratee" %% "iteratee-task" % "0.2.0-SNAPSHOT"
  )
  .dependsOn(core, jawn)

lazy val jackson = project
  .settings(
    description := "circe jackson",
    moduleName := "circe-jackson"
  )
  .settings(allSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-core" % "2.5.3",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.5.3"
    )
  )
  .dependsOn(core)

lazy val optics = project
  .settings(
    description := "circe optics",
    moduleName := "circe-optics"
  )
  .settings(allSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.julien-truffaut" %% "monocle-core" % "1.2.0",
      "com.github.julien-truffaut" %% "monocle-law" % "1.2.0" % "test"
    )
  )
  .dependsOn(core, tests % "test")

lazy val async = project
  .settings(
    description := "circe async",
    moduleName := "circe-async"
  )
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    libraryDependencies += "com.twitter" %% "util-core" % "6.29.0"
  )
  .dependsOn(core, jawn)

lazy val benchmark = project
  .settings(
    description := "circe benchmark",
    moduleName := "circe-benchmark"
  )
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.play" %% "play-json" % "2.3.10",
      "io.argonaut" %% "argonaut" % "6.1",
      "io.spray" %% "spray-json" % "1.3.2",
      "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
    )
  )
  .enablePlugins(JmhPlugin)
  .dependsOn(core, generic, jawn, jackson)

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
  "core",
  "generic",
  "refined",
  "parse",
  "tests",
  "jawn",
  "jackson",
  "async",
  "benchmark"
)

val jsProjects = Seq(
  "coreJS",
  "genericJS",
  "refinedJS",
  "parseJS",
  "testsJS"
)

addCommandAlias("buildJVM", jvmProjects.map(";" + _ + "/compile").mkString)
addCommandAlias("validateJVM", ";buildJVM;tests/test;optics/test;benchmark/test;scalastyle;unidoc")
addCommandAlias("buildJS", jsProjects.map(";" + _ + "/compile").mkString)
addCommandAlias("validateJS", ";buildJS;testsJS/test;scalastyle")
addCommandAlias("validate", ";validateJVM;validateJS")
