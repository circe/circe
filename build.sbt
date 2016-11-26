import sbtunidoc.Plugin.UnidocKeys._
import ReleaseTransformations._
import com.typesafe.sbt.SbtGhPages.GhPagesKeys._
import org.scalajs.sbtplugin.cross.{ CrossProject, CrossType }

lazy val buildSettings = Seq(
  organization := "io.circe",
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.0")
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

lazy val catsVersion = "0.8.1"
lazy val jawnVersion = "0.10.4"
lazy val shapelessVersion = "2.3.2"
lazy val refinedVersion = "0.6.0"

lazy val scalaTestVersion = "3.0.0"
lazy val scalaCheckVersion = "1.13.4"
lazy val disciplineVersion = "0.7.2"

lazy val previousCirceVersion = "0.6.1"

lazy val baseSettings = Seq(
  scalacOptions ++= compilerOptions ++ (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, p)) if p >= 11 => Seq("-Ywarn-unused-import")
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
  coverageHighlighting := (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 10)) => false
      case _ => true
    }
  ),
  coverageScalacPluginVersion := "1.3.0",
  (scalastyleSources in Compile) ++= (unmanagedSourceDirectories in Compile).value,
  ivyConfigurations += config("compile-time").hide,
  unmanagedClasspath in Compile ++= update.value.select(configurationFilter("compile-time")),
  unmanagedClasspath in Test ++= update.value.select(configurationFilter("compile-time"))
)

lazy val allSettings = buildSettings ++ baseSettings ++ publishSettings

def circeProject(path: String)(project: Project) = {
  val docName = path.split("-").mkString(" ")
  project.settings(
    description := s"circe $docName",
    moduleName := s"circe-$path",
    name := s"Circe $docName",
    allSettings
  )
}

def circeModule(path: String): Project = {
  val id = path.split("-").reduce(_ + _.capitalize)
  Project(id, file(s"modules/$path"))
    .configure(circeProject(path))
}

def circeCrossModule(path: String, crossType: CrossType = CrossType.Full) = {
  val id = path.split("-").reduce(_ + _.capitalize)
  CrossProject(jvmId = id, jsId = id + "JS", file(s"modules/$path"), crossType)
    .settings(allSettings)
    .configureAll(circeProject(path))
}

/**
 * We omit all Scala.js projects from Unidoc generation, as well as
 * circe-generic on 2.10, since Unidoc doesn't like its macros.
 */
def noDocProjects(sv: String): Seq[ProjectReference] = Seq[ProjectReference](
  coreJS,
  hygiene,
  java8,
  literalJS,
  genericJS,
  genericExtrasJS,
  shapesJS,
  numbersJS,
  opticsJS,
  parserJS,
  refinedJS,
  scodecJS,
  testingJS,
  tests,
  testsJS,
  spray,
  benchmark
) ++ (
  CrossVersion.partialVersion(sv) match {
    case Some((2, 10)) => Seq[ProjectReference](generic, literal)
    case _ => Nil
  }
)

lazy val docSettings = allSettings ++ unidocSettings ++ Seq(
  micrositeName := "circe",
  micrositeDescription := "A JSON library for Scala powered by Cats",
  micrositeAuthor := "Travis Brown",
  micrositeHighlightTheme := "atom-one-light",
  micrositeHomepage := "https://circe.github.io/circe/",
  micrositeBaseUrl := "circe",
  micrositeDocumentationUrl := "api",
  micrositeGithubOwner := "circe",
  micrositeGithubRepo := "circe",
  micrositeExtraMdFiles := Map(file("CONTRIBUTING.md") -> "contributing.md"),
  micrositePalette := Map(
    "brand-primary" -> "#5B5988",
    "brand-secondary" -> "#292E53",
    "brand-tertiary" -> "#222749",
    "gray-dark" -> "#49494B",
    "gray" -> "#7B7B7E",
    "gray-light" -> "#E5E5E6",
    "gray-lighter" -> "#F4F3F4",
    "white-color" -> "#FFFFFF"),
  addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), micrositeDocumentationUrl),
  ghpagesNoJekyll := false,
  scalacOptions in (ScalaUnidoc, unidoc) ++= Seq(
    "-groups",
    "-implicits",
    "-doc-source-url", scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
    "-sourcepath", baseDirectory.in(LocalRootProject).value.getAbsolutePath
  ),
  git.remoteRepo := "git@github.com:circe/circe.git",
  unidocProjectFilter in (ScalaUnidoc, unidoc) :=
    inAnyProject -- inProjects(noDocProjects(scalaVersion.value): _*),
  includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.svg" | "*.js" | "*.swf" | "*.yml" | "*.md"
)

lazy val docs = project.dependsOn(core, generic, parser, optics)
  .settings(
    moduleName := "circe-docs",
    name := "Circe docs"
  )
  .settings(docSettings)
  .settings(noPublishSettings)
  .settings(
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
  )
  .enablePlugins(MicrositesPlugin)

lazy val aggregatedProjects: Seq[ProjectReference] = Seq[ProjectReference](
  numbers, numbersJS,
  core, coreJS,
  generic, genericJS,
  genericExtras, genericExtrasJS,
  shapes, shapesJS,
  literal, literalJS,
  refined, refinedJS,
  parser, parserJS,
  scodec, scodecJS,
  testing, testingJS,
  tests, testsJS,
  jawn,
  jackson,
  optics, opticsJS,
  scalajs,
  streaming,
  docs,
  spray,
  hygiene,
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
  .enablePlugins(CrossPerProjectPlugin)
  .settings(allSettings)
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
      """.stripMargin
  )
  .aggregate(aggregatedProjects: _*)
  .dependsOn(core, genericExtras, literal, parser)

lazy val numbersBase = circeCrossModule("numbers")
  .settings(
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck" % scalaCheckVersion % "test",
      "org.scalatest" %%% "scalatest" % scalaTestVersion % "test"
    )
  )
  .jvmSettings(
    mimaPreviousArtifacts := Set("io.circe" %% "circe-numbers" % previousCirceVersion)
  )

lazy val numbers = numbersBase.jvm
lazy val numbersJS = numbersBase.js

lazy val coreBase = circeCrossModule("core")
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % catsVersion
    ),
    sourceGenerators in Compile += (sourceManaged in Compile).map(Boilerplate.gen).taskValue
  )
  .jvmSettings(
    mimaPreviousArtifacts := Set("io.circe" %% "circe-core" % previousCirceVersion)
  )
  .dependsOn(numbersBase)

lazy val core = coreBase.jvm
lazy val coreJS = coreBase.js

lazy val genericBase = circeCrossModule("generic")
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
  .jvmSettings(
    mimaPreviousArtifacts := Set("io.circe" %% "circe-generic" % previousCirceVersion)
  )
  .dependsOn(coreBase)

lazy val generic = genericBase.jvm
lazy val genericJS = genericBase.js

lazy val genericExtrasBase = circeCrossModule("generic-extras", CrossType.Pure)
  .settings(macroDependencies: _*)
  .settings(
    sources in (Compile, doc) := (
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 11)) => (sources in (Compile, doc)).value
        case _ => Nil
      }
    )
  )
  .jvmSettings(
    mimaPreviousArtifacts := Set("io.circe" %% "circe-generic-extras" % previousCirceVersion)
  )
  .dependsOn(genericBase)

lazy val genericExtras = genericExtrasBase.jvm
lazy val genericExtrasJS = genericExtrasBase.js

lazy val shapesBase = circeCrossModule("shapes", CrossType.Pure)
  .settings(macroDependencies: _*)
  .settings(
    libraryDependencies += "com.chuusai" %%% "shapeless" % shapelessVersion
  )
  .jvmSettings(
    mimaPreviousArtifacts := Set("io.circe" %% "circe-shapes" % previousCirceVersion)
  )
  .dependsOn(coreBase)

lazy val shapes = shapesBase.jvm
lazy val shapesJS = shapesBase.js

lazy val literalBase = circeCrossModule("literal", CrossType.Pure)
  .settings(macroDependencies: _*)
  .settings(
    sources in (Compile, doc) := (
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 11)) => (sources in (Compile, doc)).value
        case _ => Nil
      }
    )
  )
  .jvmSettings(
    mimaPreviousArtifacts := Set("io.circe" %% "circe-literal" % previousCirceVersion)
  )
  .dependsOn(coreBase)

lazy val literal = literalBase.jvm
lazy val literalJS = literalBase.js

lazy val refinedBase = circeCrossModule("refined")
  .settings(
    libraryDependencies += "eu.timepit" %%% "refined" % refinedVersion
  )
  .jvmSettings(
    mimaPreviousArtifacts := Set("io.circe" %% "circe-refined" % previousCirceVersion)
  )
  .dependsOn(coreBase)

lazy val refined = refinedBase.jvm
lazy val refinedJS = refinedBase.js

lazy val parserBase = circeCrossModule("parser")
  .jvmSettings(
    mimaPreviousArtifacts := Set("io.circe" %% "circe-parser" % previousCirceVersion)
  )
  .jvmConfigure(_.dependsOn(jawn))
  .jsConfigure(_.dependsOn(scalajs))
  .dependsOn(coreBase)

lazy val parser = parserBase.jvm
lazy val parserJS = parserBase.js

lazy val scalajs = circeModule("scalajs")
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(coreJS)

lazy val scodecBase = circeCrossModule("scodec")
  .settings(
    libraryDependencies += "org.scodec" %%% "scodec-bits" % "1.1.2"
  )
  .jvmSettings(
    mimaPreviousArtifacts := Set("io.circe" %% "circe-scodec" % previousCirceVersion)
  )
  .dependsOn(coreBase)

lazy val scodec = scodecBase.jvm
lazy val scodecJS = scodecBase.js

lazy val testingBase = circeCrossModule("testing")
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % scalaTestVersion,
      "org.typelevel" %%% "cats-laws" % catsVersion,
      "org.typelevel" %%% "discipline" % disciplineVersion
    )
  )
  .settings(
    coverageExcludedPackages := "io\\.circe\\.testing\\..*"
  )
  .jvmSettings(
    mimaPreviousArtifacts := Set("io.circe" %% "circe-testing" % previousCirceVersion)
  )
  .dependsOn(coreBase)

lazy val testing = testingBase.jvm
lazy val testingJS = testingBase.js

lazy val testsBase = circeCrossModule("tests")
  .settings(noPublishSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "com.chuusai" %%% "shapeless" % shapelessVersion,
      "org.scalacheck" %%% "scalacheck" % scalaCheckVersion,
      "org.scalatest" %%% "scalatest" % scalaTestVersion,
      "org.scodec" %%% "scodec-bits" % "1.1.0",
      "org.typelevel" %%% "cats-laws" % catsVersion,
      "org.typelevel" %%% "discipline" % disciplineVersion,
      "eu.timepit" %%% "refined-scalacheck" % refinedVersion,
      compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
    ),
    sourceGenerators in Test += (sourceManaged in Test).map(Boilerplate.genTests).taskValue,
    unmanagedResourceDirectories in Compile +=
      file("modules/tests") / "shared" / "src" / "main" / "resources"
  )
  .settings(
    coverageExcludedPackages := "io\\.circe\\.tests\\..*"
  )
  .jvmSettings(fork := true)
  .jvmConfigure(_.dependsOn(jawn, jackson, streaming))
  .jsConfigure(
    _.settings(
      libraryDependencies += "org.spire-math" %% "jawn-parser" % jawnVersion % "compile-time"
    ).dependsOn(scalajs)
  )
  .dependsOn(
    testingBase,
    coreBase,
    genericBase,
    genericExtrasBase,
    shapesBase,
    literalBase,
    refinedBase,
    parserBase,
    scodecBase
  )

lazy val tests = testsBase.jvm
lazy val testsJS = testsBase.js

lazy val hygiene = circeModule("hygiene")
  .settings(noPublishSettings)
  .settings(
    crossScalaVersions := crossScalaVersions.value.tail,
    scalacOptions ++= Seq("-Yno-imports", "-Yno-predef")
  )
  .dependsOn(core, generic, jawn, literal)

lazy val jawn = circeModule("jawn")
  .settings(
    libraryDependencies += "org.spire-math" %% "jawn-parser" % jawnVersion,
    mimaPreviousArtifacts := Set("io.circe" %% "circe-jawn" % previousCirceVersion)
  )
  .dependsOn(core)

lazy val java8 = circeModule("java8")
  .settings(
    mimaPreviousArtifacts := Set("io.circe" %% "circe-java8" % previousCirceVersion)
  )
  .dependsOn(core, tests % "test")

lazy val streaming = circeModule("streaming")
  .settings(
    libraryDependencies += "io.iteratee" %% "iteratee-core" % "0.7.1",
    mimaPreviousArtifacts := Set("io.circe" %% "circe-streaming" % previousCirceVersion)
  )
  .dependsOn(core, jawn)

lazy val jackson = circeModule("jackson")
  .settings(
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-core" % "2.5.3",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.5.3"
    ),
    mimaPreviousArtifacts := Set("io.circe" %% "circe-jackson" % previousCirceVersion)
  )
  .dependsOn(core)

lazy val spray = circeModule("spray")
  .settings(
    crossScalaVersions := crossScalaVersions.value.init,
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
    ),
    mimaPreviousArtifacts := Set("io.circe" %% "circe-spray" % previousCirceVersion)
  )
  .dependsOn(core, jawn, generic % "test")

lazy val opticsBase = circeCrossModule("optics", CrossType.Pure)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.julien-truffaut" %%% "monocle-core" % "1.3.2",
      "com.github.julien-truffaut" %%% "monocle-law" % "1.3.2" % "test",
      compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
    )
  )
  .jvmSettings(mimaPreviousArtifacts := Set("io.circe" %% "circe-optics" % previousCirceVersion))
  .dependsOn(coreBase, testsBase % "test")

lazy val optics = opticsBase.jvm
lazy val opticsJS = opticsBase.js

lazy val benchmark = circeModule("benchmark")
  .settings(noPublishSettings)
  .settings(
    crossScalaVersions := crossScalaVersions.value.init,
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
  homepage := Some(url("https://github.com/circe/circe")),
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
  apiURL := Some(url("https://circe.github.io/circe/api/")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/circe/circe"),
      "scm:git:git@github.com:circe/circe.git"
    )
  ),
  developers := List(
    Developer("travisbrown", "Travis Brown", "@travisbrown",
      url("https://twitter.com/travisbrown"))
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
  "genericExtras",
  "shapes",
  "refined",
  "parser",
  "scodec",
  "tests",
  "jawn",
  "jackson"
) ++ (
  if (sys.props("java.specification.version") == "1.8") Seq("java8", "optics") else Nil
)

val jvmTestProjects = Seq(
  "numbers",
  "tests"
) ++ (
  if (sys.props("java.specification.version") == "1.8") Seq("java8", "optics") else Nil
)

val jsProjects = Seq(
  "numbersJS",
  "coreJS",
  "genericJS",
  "genericExtrasJS",
  "shapesJS",
  "opticsJS",
  "parserJS",
  "refinedJS",
  "scalajs",
  "scodecJS",
  "testsJS"
)

addCommandAlias("buildJVM", jvmProjects.map(";" + _ + "/compile").mkString)
addCommandAlias(
  "validateJVM",
  ";buildJVM" + jvmTestProjects.map(";" + _ + "/test").mkString + ";scalastyle;unidoc"
)
addCommandAlias("buildJS", jsProjects.map(";" + _ + "/compile").mkString)
addCommandAlias("validateJS", ";buildJS;opticsJS/test;testsJS/test;scalastyle")
addCommandAlias("validate", ";validateJVM;validateJS")
