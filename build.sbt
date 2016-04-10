import sbtunidoc.Plugin.UnidocKeys._
import ReleaseTransformations._

lazy val buildSettings = Seq(
  organization := "io.circe",
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.10.6", "2.11.8")
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

lazy val botBuild = settingKey[Boolean]("Build by TravisCI instead of local dev environment")

lazy val catsVersion = "0.4.1"
lazy val jawnVersion = "0.8.4"
lazy val shapelessVersion = "2.3.0"
lazy val refinedVersion = "0.3.7"

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
  scalacOptions in (Compile, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import"))
  },
  scalacOptions in (Test, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import"))
  },
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
// Using Rhino as jsEnv to build scala.js code can lead to OOM, switch to PhantomJS by default
  scalaJSUseRhino := false,
  requiresDOM := false,
  jsEnv := NodeJSEnv().value,
  // Only used for scala.js for now
  botBuild := sys.props.getOrElse("CIRCE_BOT_BUILD", default="false") == "true",
  // batch mode decreases the amount of memory needed to compile scala.js code
  scalaJSOptimizerOptions := scalaJSOptimizerOptions.value.withBatchMode(botBuild.value)
)

/**
 * We omit all Scala.js projects from Unidoc generation, as well as
 * circe-generic on 2.10, since Unidoc doesn't like its macros.
 */
def noDocProjects(sv: String): Seq[ProjectReference] = Seq[ProjectReference](
  benchmark,
  coreJS,
  java8,
  literalJS,
  genericJS,
  numbersJS,
  parserJS,
  refinedJS,
  tests,
  testsJS
) ++ (
  CrossVersion.partialVersion(sv) match {
    case Some((2, 10)) => Seq[ProjectReference](generic, literal)
    case _ => Nil
  }
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
    inAnyProject -- inProjects(noDocProjects(scalaVersion.value): _*)
)

lazy val aggregatedProjects: Seq[ProjectReference] = Seq[ProjectReference](
  numbers, numbersJS,
  core, coreJS,
  generic, genericJS,
  literal, literalJS,
  refined, refinedJS,
  parser, parserJS,
  tests, testsJS,
  jawn,
  jackson,
  optics,
  scalajs,
  spray,
  streaming,
  benchmark
) ++ (
  if (sys.props("java.specification.version") == "1.8") Seq[ProjectReference](java8) else Nil
)

lazy val macroDependencies: Seq[Setting[_]] = Seq(
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % "provided",
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % "provided",
    "org.typelevel" %%% "macro-compat" % "1.1.1",
    compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
  ),
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      // if scala 2.11+ is used, quasiquotes are merged into scala-reflect.
      case Some((2, scalaMajor)) if scalaMajor >= 11 => Nil
      // in Scala 2.10, quasiquotes are provided by macro paradise.
      case Some((2, 10)) => Seq("org.scalamacros" %% "quasiquotes" % "2.1.0" cross CrossVersion.binary)
    }
  }
)

lazy val circe = project.in(file("."))
  .settings(allSettings)
  .settings(docSettings)
  .settings(noPublishSettings)
  .settings(
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
    initialCommands in console :=
      """
        |import io.circe._
        |import io.circe.generic.auto._
        |import io.circe.literal._
        |import io.circe.parser._
        |import io.circe.syntax._
        |import cats.data.Xor
      """.stripMargin
  )
  .aggregate(aggregatedProjects: _*)
  .dependsOn(core, generic, literal, parser)

lazy val numbersBase = crossProject.in(file("numbers"))
  .settings(
    description := "circe numbers",
    moduleName := "circe-numbers",
    name := "numbers"
  )
  .settings(allSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck" % scalaCheckVersion % "test",
      "org.scalatest" %%% "scalatest" % scalaTestVersion % "test"
    )
  )
  .jsSettings(commonJsSettings: _*)
  .jvmConfigure(_.copy(id = "numbers"))
  .jsConfigure(_.copy(id = "numbersJS"))

lazy val numbers = numbersBase.jvm
lazy val numbersJS = numbersBase.js

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
  .dependsOn(numbersBase)

lazy val core = coreBase.jvm
lazy val coreJS = coreBase.js

lazy val genericBase = crossProject.in(file("generic"))
  .settings(
    description := "circe generic",
    moduleName := "circe-generic",
    name := "generic"
  )
  .settings(allSettings: _*)
  .settings(macroDependencies: _*)
  .settings(
    libraryDependencies += "com.chuusai" %%% "shapeless" % shapelessVersion,
    sources in (Compile, doc) := (
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 11)) => (sources in (Compile, doc)).value
        case _ => Nil
      }
    )
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
  .settings(macroDependencies: _*)
  .settings(
    sources in (Compile, doc) := (
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 11)) => (sources in (Compile, doc)).value
        case _ => Nil
      }
    )
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

lazy val parserBase = crossProject.in(file("parser"))
  .settings(
    description := "circe parser",
    moduleName := "circe-parser",
    name := "parser"
  )
  .settings(allSettings: _*)
  .jsSettings(commonJsSettings: _*)
  .jvmConfigure(_.copy(id = "parser").dependsOn(jawn))
  .jsConfigure(_.copy(id = "parserJS").dependsOn(scalajs))
  .dependsOn(coreBase)

lazy val parser = parserBase.jvm
lazy val parserJS = parserBase.js

lazy val scalajs = project
  .settings(
    description := "circe scalajs",
    moduleName := "circe-scalajs"
  )
  .settings(allSettings)
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(coreJS)

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
      "eu.timepit" %%% "refined-scalacheck" % refinedVersion,
      compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
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
  .jvmConfigure(_.copy(id = "tests").dependsOn(jawn, jackson, streaming))
  .jsConfigure(
    _.copy(id = "testsJS").settings(
      libraryDependencies += "org.spire-math" %% "jawn-parser" % jawnVersion % "compile-time"
    ).dependsOn(scalajs)
  )
  .dependsOn(
    numbersBase % "compile->test",
    coreBase,
    genericBase,
    literalBase,
    refinedBase,
    parserBase
  )

lazy val tests = testsBase.jvm
lazy val testsJS = testsBase.js

lazy val jawn = project
  .settings(
    description := "circe jawn",
    moduleName := "circe-jawn"
  )
  .settings(allSettings)
  .settings(
    libraryDependencies += "org.spire-math" %% "jawn-parser" % jawnVersion
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
    libraryDependencies += "io.iteratee" %% "iteratee-core" % "0.3.1"
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

lazy val spray = project
  .settings(
    description := "circe spray",
    moduleName := "circe-spray"
  )
  .settings(allSettings)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.3.9",
      "io.spray" %% "spray-httpx" % "1.3.3",
      /**
       * spray-routing-shapeless2 depends on Shapeless 2.1, which uses a
       * different suffix for Scala 2.10 than Shapeless 2.3 (the version brought
       * in by circe-generic). Since this is only a test dependency, we simply
       * exclude the transitive Shapeless 2.1 dependency to avoid conflicting
       * cross-version suffixes on 2.10.
       */
      "io.spray" %% "spray-routing-shapeless2" % "1.3.3" % "test" exclude("com.chuusai", "shapeless_2.10.4"),
      "io.spray" %% "spray-testkit" % "1.3.3" % "test",
      "org.scalacheck" %% "scalacheck" % scalaCheckVersion % "test",
      "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
      compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" % "test" cross CrossVersion.full)
    )
  )
  .dependsOn(core, jawn, generic % "test")

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
      "io.github.netvl.picopickle" %% "picopickle-core" % "0.2.1",
      "io.github.netvl.picopickle" %% "picopickle-backend-jawn" % "0.2.1",
      "org.scalatest" %% "scalatest" % scalaTestVersion % "test",
      compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
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
  "numbers",
  "core",
  "generic",
  "refined",
  "parser",
  "tests",
  "jawn",
  "jackson",
  "spray",
  "benchmark"
) ++ (
  if (sys.props("java.specification.version") == "1.8") Seq("java8") else Nil
)

val jvmTestProjects = Seq(
  "tests",
  "optics",
  "spray",
  "benchmark"
) ++ (
  if (sys.props("java.specification.version") == "1.8") Seq("java8") else Nil
)

val jsProjects = Seq(
  "numbersJS",
  "coreJS",
  "genericJS",
  "refinedJS",
  "parserJS",
  "scalajs",
  "testsJS"
)

addCommandAlias("buildJVM", jvmProjects.map(";" + _ + "/compile").mkString)
addCommandAlias("validateJVM", ";buildJVM" + jvmTestProjects.map(";" + _ + "/test").mkString + ";scalastyle;unidoc")
addCommandAlias("buildJS", jsProjects.map(";" + _ + "/compile").mkString)
addCommandAlias("validateJS", ";buildJS;testsJS/test;scalastyle")
addCommandAlias("validate", ";validateJVM;validateJS")
