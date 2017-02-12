import ReleaseTransformations._
import com.typesafe.sbt.SbtGhPages.GhPagesKeys._
import microsites.ExtraMdFileConfig
import org.scalajs.sbtplugin.cross.{ CrossProject, CrossType }

organization in ThisBuild := "io.circe"

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
  "-Xfuture",
  "-Yno-predef"
)

lazy val catsVersion = "0.9.0"
lazy val jawnVersion = "0.10.4"
lazy val shapelessVersion = "2.3.2"
lazy val refinedVersion = "0.6.2"
lazy val monocleVersion = "1.4.0"

lazy val scalaTestVersion = "3.0.1"
lazy val scalaCheckVersion = "1.13.4"
lazy val disciplineVersion = "0.7.3"

lazy val previousCirceVersion = Some("0.6.1")

lazy val baseSettings = Seq(
  scalacOptions ++= compilerOptions ++ (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, p)) if p >= 11 => Seq("-Ywarn-unused-import")
      case _ => Nil
    }
  ),
  scalacOptions in (Compile, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Yno-predef"))
  },
  scalacOptions in (Test, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Yno-predef"))
  },
  scalacOptions in Test ~= {
    _.filterNot(Set("-Yno-predef"))
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

lazy val allSettings = baseSettings ++ publishSettings

def circeProject(path: String)(project: Project) = {
  val docName = path.split("-").mkString(" ")
  project.settings(
    description := s"circe $docName",
    moduleName := s"circe-$path",
    name := s"Circe $docName",
    allSettings
  )
}

def circeModule(path: String, mima: Option[String]): Project = {
  val id = path.split("-").reduce(_ + _.capitalize)
  Project(id, file(s"modules/$path"))
    .configure(circeProject(path))
    .settings(mimaPreviousArtifacts := mima.map("io.circe" %% moduleName.value % _).toSet)
}

def circeCrossModule(path: String, mima: Option[String], crossType: CrossType = CrossType.Full) = {
  val id = path.split("-").reduce(_ + _.capitalize)
  CrossProject(jvmId = id, jsId = id + "JS", file(s"modules/$path"), crossType)
    .settings(allSettings)
    .configureAll(circeProject(path))
    .jvmSettings(
      mimaPreviousArtifacts := mima.map("io.circe" %% moduleName.value % _).toSet
    )
}

/**
 * We omit all Scala.js projects from Unidoc generation, as well as
 * circe-generic on 2.10, since Unidoc doesn't like its macros.
 */
def noDocProjects(sv: String): Seq[ProjectReference] = {
  val unwanted = circeCrossModules.map(_._2) ++ circeUtilModules :+ tests
  val scala210 = CrossVersion.partialVersion(sv) match {
    case Some((2, 10)) => Seq(generic, literal)
    case _ => Nil
  }

  (unwanted ++ jvm8Only(java8) ++ scala210).map(p => p: ProjectReference)
}

lazy val docSettings = allSettings ++ Seq(
  micrositeName := "circe",
  micrositeDescription := "A JSON library for Scala powered by Cats",
  micrositeAuthor := "Travis Brown",
  micrositeHighlightTheme := "atom-one-light",
  micrositeHomepage := "https://circe.github.io/circe/",
  micrositeBaseUrl := "circe",
  micrositeDocumentationUrl := "api",
  micrositeGithubOwner := "circe",
  micrositeGithubRepo := "circe",
  micrositeExtraMdFiles := Map(file("CONTRIBUTING.md") -> ExtraMdFileConfig("contributing.md", "docs")),
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
    "-skip-packages", "scalaz",
    "-doc-source-url", scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
    "-sourcepath", baseDirectory.in(LocalRootProject).value.getAbsolutePath,
    "-doc-root-content", (resourceDirectory.in(Compile).value / "rootdoc.txt").getAbsolutePath
  ),
  scalacOptions ~= {
    _.filterNot(Set("-Yno-predef"))
  },
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
  .enablePlugins(ScalaUnidocPlugin)

lazy val circeCrossModules = Seq[(Project, Project)](
  (numbers, numbersJS),
  (core, coreJS),
  (generic, genericJS),
  (genericExtras, genericExtrasJS),
  (shapes, shapesJS),
  (literal, literalJS),
  (optics, opticsJS),
  (refined, refinedJS),
  (parser, parserJS),
  (scodec, scodecJS),
  (testing, testingJS),
  (tests, testsJS)
)

lazy val circeJsModules = Seq[Project](scalajs)

lazy val circeJvmModules = Seq[Project](
  jawn,
  java8,
  streaming
)

lazy val circeDocsModules = Seq[Project](docs)

lazy val circeUtilModules = Seq[Project](hygiene, benchmark)

def jvm8Only(projects: Project*): Set[Project] = sys.props("java.specification.version") match {
  case "1.8" => Set.empty
  case _ => Set(projects: _*)
}

lazy val jvmProjects: Seq[Project] =
  (circeCrossModules.map(_._1) ++ circeJvmModules).filterNot(jvm8Only(java8))

lazy val jsProjects: Seq[Project] =
  (circeCrossModules.map(_._2) ++ circeJsModules)

/**
 * Aggregation should ensure that publish works as expected on the given
 * JVM version. The `validate` command aliases will filter out projects
 * not supported by the given JVM.
 */
lazy val aggregatedProjects: Seq[ProjectReference] =
  (circeCrossModules.flatMap(cp => Seq(cp._1, cp._2)) ++
   circeJsModules ++ circeJvmModules ++ circeDocsModules)
    .filterNot(jvm8Only(java8)).map(p => p: ProjectReference)

def macroSettings(scaladocFor210: Boolean): Seq[Setting[_]] = Seq(
  libraryDependencies ++= Seq(
    "org.scala-lang" % "scala-compiler" % scalaVersion.value % Provided,
    "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided,
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
  },
  sources in (Compile, doc) := {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 10)) if !scaladocFor210 => Nil
      case _ => (sources in (Compile, doc)).value
    }
  }
)

lazy val circe = project.in(file("."))
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

lazy val numbersBase = circeCrossModule("numbers", mima = previousCirceVersion)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck" % scalaCheckVersion % Test,
      "org.scalatest" %%% "scalatest" % scalaTestVersion % Test
    )
  )

lazy val numbers = numbersBase.jvm
lazy val numbersJS = numbersBase.js

lazy val coreBase = circeCrossModule("core", mima = previousCirceVersion)
  .settings(
    libraryDependencies += "org.typelevel" %%% "cats-core" % catsVersion,
    sourceGenerators in Compile += (sourceManaged in Compile).map(Boilerplate.gen).taskValue
  )
  .dependsOn(numbersBase)

lazy val core = coreBase.jvm
lazy val coreJS = coreBase.js

lazy val genericBase = circeCrossModule("generic", mima = previousCirceVersion)
  .settings(macroSettings(scaladocFor210 = false))
  .settings(
    libraryDependencies += "com.chuusai" %%% "shapeless" % shapelessVersion
  )
  .dependsOn(coreBase)

lazy val generic = genericBase.jvm
lazy val genericJS = genericBase.js

lazy val genericExtrasBase = circeCrossModule("generic-extras", mima = previousCirceVersion, CrossType.Pure)
  .settings(macroSettings(scaladocFor210 = false))
  .dependsOn(genericBase)

lazy val genericExtras = genericExtrasBase.jvm
lazy val genericExtrasJS = genericExtrasBase.js

lazy val shapesBase = circeCrossModule("shapes", mima = previousCirceVersion, CrossType.Pure)
  .settings(macroSettings(scaladocFor210 = true))
  .settings(
    libraryDependencies += "com.chuusai" %%% "shapeless" % shapelessVersion
  )
  .dependsOn(coreBase)

lazy val shapes = shapesBase.jvm
lazy val shapesJS = shapesBase.js

lazy val literalBase = circeCrossModule("literal", mima = previousCirceVersion, CrossType.Pure)
  .settings(macroSettings(scaladocFor210 = false))
  .dependsOn(coreBase)

lazy val literal = literalBase.jvm
lazy val literalJS = literalBase.js

lazy val refinedBase = circeCrossModule("refined", mima = previousCirceVersion)
  .settings(
    libraryDependencies += "eu.timepit" %%% "refined" % refinedVersion
  )
  .dependsOn(coreBase)

lazy val refined = refinedBase.jvm
lazy val refinedJS = refinedBase.js

lazy val parserBase = circeCrossModule("parser", mima = previousCirceVersion)
  .jvmConfigure(_.dependsOn(jawn))
  .jsConfigure(_.dependsOn(scalajs))
  .dependsOn(coreBase)

lazy val parser = parserBase.jvm
lazy val parserJS = parserBase.js

lazy val scalajs = circeModule("scalajs", mima = None)
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(coreJS)

lazy val scodecBase = circeCrossModule("scodec", mima = previousCirceVersion)
  .settings(
    libraryDependencies += "org.scodec" %%% "scodec-bits" % "1.1.4"
  )
  .dependsOn(coreBase)

lazy val scodec = scodecBase.jvm
lazy val scodecJS = scodecBase.js

lazy val testingBase = circeCrossModule("testing", mima = previousCirceVersion)
  .settings(
    scalacOptions ~= {
      _.filterNot(Set("-Yno-predef"))
    },
    libraryDependencies ++= Seq(
      "org.scalatest" %%% "scalatest" % scalaTestVersion,
      "org.typelevel" %%% "cats-laws" % catsVersion,
      "org.typelevel" %%% "discipline" % disciplineVersion
    )
  )
  .settings(
    coverageExcludedPackages := "io\\.circe\\.testing\\..*"
  )
  .dependsOn(coreBase)

lazy val testing = testingBase.jvm
lazy val testingJS = testingBase.js

lazy val testsBase = circeCrossModule("tests", mima = None)
  .settings(noPublishSettings: _*)
  .settings(
    scalacOptions ~= {
      _.filterNot(Set("-Yno-predef"))
    },
    libraryDependencies ++= Seq(
      "com.chuusai" %%% "shapeless" % shapelessVersion,
      "org.scalacheck" %%% "scalacheck" % scalaCheckVersion,
      "org.scalatest" %%% "scalatest" % scalaTestVersion,
      "org.scodec" %%% "scodec-bits" % "1.1.4",
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
  .jvmConfigure(_.dependsOn(jawn, streaming))
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

lazy val hygiene = circeModule("hygiene", mima = None)
  .settings(noPublishSettings)
  .settings(
    crossScalaVersions := crossScalaVersions.value.tail,
    scalacOptions ++= Seq("-Yno-imports", "-Yno-predef")
  )
  .dependsOn(core, generic, jawn, literal)

lazy val jawn = circeModule("jawn", mima = previousCirceVersion)
  .settings(
    libraryDependencies += "org.spire-math" %% "jawn-parser" % jawnVersion
  )
  .dependsOn(core)

lazy val java8 = circeModule("java8", mima = previousCirceVersion)
  .dependsOn(core, tests % Test)

lazy val streaming = circeModule("streaming", mima = previousCirceVersion)
  .settings(
    libraryDependencies += "io.iteratee" %% "iteratee-core" % "0.9.0"
  )
  .dependsOn(core, jawn)

lazy val opticsBase = circeCrossModule("optics", mima = previousCirceVersion, CrossType.Pure)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.julien-truffaut" %%% "monocle-core" % monocleVersion,
      "com.github.julien-truffaut" %%% "monocle-law"  % monocleVersion % Test,
      compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
    )
  )
  .dependsOn(coreBase, testsBase % Test)

lazy val optics = opticsBase.jvm
lazy val opticsJS = opticsBase.js

lazy val benchmark = circeModule("benchmark", mima = None)
  .settings(noPublishSettings)
  .settings(
    crossScalaVersions := crossScalaVersions.value.init,
    scalacOptions ~= {
      _.filterNot(Set("-Yno-predef"))
    },
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
      compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
    )
  )
  .enablePlugins(JmhPlugin)
  .dependsOn(core, generic, jawn)

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
    Developer("travisbrown", "Travis Brown", "travisrobertbrown@gmail.com",
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

val jvmTestProjects = Seq(numbers, tests, java8, optics).filterNot(jvm8Only(java8))

addCommandAlias("buildJVM", jvmProjects.map(";" + _.id + "/compile").mkString)
addCommandAlias(
  "validateJVM",
  ";buildJVM" + jvmTestProjects.map(";" + _.id + "/test").mkString + ";scalastyle;unidoc"
)
addCommandAlias("buildJS", jsProjects.map(";" + _.id + "/compile").mkString)
addCommandAlias("validateJS", ";buildJS;opticsJS/test;testsJS/test;scalastyle")
addCommandAlias("validate", ";validateJVM;validateJS")
