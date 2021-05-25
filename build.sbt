import ReleaseTransformations._
import microsites.ExtraMdFileConfig
import microsites.ConfigYml
import sbtcrossproject.{ CrossProject, CrossType }
import scala.xml.{ Elem, Node => XmlNode, NodeSeq => XmlNodeSeq }
import scala.xml.transform.{ RewriteRule, RuleTransformer }

ThisBuild / organization := "io.circe"
ThisBuild / crossScalaVersions := List("3.0.0", "2.12.13", "2.13.6")
ThisBuild / scalaVersion := crossScalaVersions.value.last

ThisBuild / githubWorkflowJavaVersions := Seq("adopt@1.8")
ThisBuild / githubWorkflowScalaVersions := (ThisBuild / crossScalaVersions).value.tail
ThisBuild / githubWorkflowPublishTargetBranches := Nil
ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep
    .Use(UseRef.Public("ruby", "setup-ruby", "v1"), params = Map("ruby-version" -> "2.7"), name = Some("Set up Ruby")),
  WorkflowStep.Run(
    List("gem install sass", "gem install jekyll -v 4.0.0"),
    name = Some("Install Jekyll")
  ),
  WorkflowStep.Sbt(
    List(
      "clean",
      "coverage",
      "scalastyle",
      "scalafmtCheckAll",
      "scalafmtSbtCheck",
      "validateJVM",
      "benchmark/test"
    ),
    id = None,
    name = Some("Test")
  ),
  WorkflowStep.Sbt(
    List("coverageReport"),
    id = None,
    name = Some("Coverage")
  ),
  WorkflowStep.Use(
    UseRef.Public(
      "codecov",
      "codecov-action",
      "v1"
    )
  )
)

val compilerOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Xfuture",
  "-Yno-predef",
  "-Ywarn-unused-import"
)

val catsVersion = "2.6.1"
val jawnVersion = "1.1.2"
val shapelessVersion = "2.3.6"
val refinedVersion = "0.9.25"

val paradiseVersion = "2.1.1"

val scalaTestVersion = "3.2.9"
val scalaCheckVersion = "1.15.4"
val munitVersion = "0.7.26"
val disciplineVersion = "1.1.5"
val disciplineScalaTestVersion = "2.1.5"
val disciplineMunitVersion = "1.0.9"
val scalaJavaTimeVersion = "2.3.0"

/**
 * Some terrible hacks to work around Cats's decision to have builds for
 * different Scala versions depend on different versions of Discipline, etc.
 */
def priorTo2_13(scalaVersion: String): Boolean =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, minor)) if minor < 13 => true
    case _                              => false
  }

val previousCirceVersion = Some("0.14.0-M3")
val scalaFiddleCirceVersion = "0.9.1"

lazy val baseSettings = Seq(
  scalacOptions ++= {
    if (priorTo2_13(scalaVersion.value)) compilerOptions
    else
      compilerOptions.flatMap {
        case "-Ywarn-unused-import" => Seq("-Ywarn-unused:imports")
        case "-Xfuture"             => Nil
        case other                  => Seq(other)
      }
  },
  Compile / console / scalacOptions ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports", "-Yno-predef"))
  },
  Test / console / scalacOptions ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports", "-Yno-predef"))
  },
  Test / scalacOptions ~= {
    _.filterNot(Set("-Yno-predef"))
  },
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  coverageHighlighting := true,
  Compile / scalastyleSources ++= (Compile / unmanagedSourceDirectories).value,
  ivyConfigurations += CompileTime.hide,
  Compile / unmanagedClasspath ++= update.value.select(configurationFilter(CompileTime.name)),
  Test / unmanagedClasspath ++= update.value.select(configurationFilter(CompileTime.name)),
  testFrameworks += new TestFramework("munit.Framework")
)

lazy val allSettings = baseSettings ++ publishSettings

lazy val jsProjectSettings = Seq(
  Test / scalaJSLinkerConfig ~= (_.withModuleKind(ModuleKind.CommonJSModule))
)

val isScala3 = Def.setting {
  CrossVersion.partialVersion(scalaVersion.value).exists(_._1 != 2)
}

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
  CrossProject(id, file(s"modules/$path"))(JVMPlatform, JSPlatform)
    .crossType(crossType)
    .settings(allSettings)
    .configure(circeProject(path))
    .jvmSettings(
      mimaPreviousArtifacts := mima.map("io.circe" %% moduleName.value % _).toSet
    )
    .jsSettings(
      coverageEnabled := false,
      jsProjectSettings,
      scalacOptions += {
        val tagOrHash =
          if (!isSnapshot.value) s"v${version.value}"
          else git.gitHeadCommit.value.getOrElse("master")
        val local = (LocalRootProject / baseDirectory).value.toURI.toString
        val remote = s"https://raw.githubusercontent.com/circe/circe/$tagOrHash/"
        val opt = if (isScala3.value) "-scalajs-mapSourceURI" else "-P:scalajs:mapSourceURI"
        s"$opt:$local->$remote"
      }
    )
}

/**
 * We omit all Scala.js projects from Unidoc generation.
 */
def noDocProjects(sv: String): Seq[ProjectReference] =
  (circeCrossModules.map(_._2) :+ tests :+ genericSimple :+ genericSimpleJS :+ benchmarkDotty).map(p =>
    p: ProjectReference
  )

lazy val docsMappingsAPIDir =
  settingKey[String]("Name of subdirectory in site target directory for api docs")

lazy val docSettings = allSettings ++ Seq(
  micrositeName := "circe",
  micrositeDescription := "A JSON library for Scala powered by Cats",
  micrositeAuthor := "Travis Brown",
  micrositeHighlightTheme := "atom-one-light",
  micrositeHomepage := "https://circe.github.io/circe/",
  micrositeBaseUrl := "/circe",
  micrositeDocumentationUrl := s"${docsMappingsAPIDir.value}/io/circe",
  micrositeDocumentationLabelDescription := "API Documentation",
  micrositeGithubOwner := "circe",
  micrositeGithubRepo := "circe",
  micrositeExtraMdFiles := Map(file("CONTRIBUTING.md") -> ExtraMdFileConfig("contributing.md", "docs")),
  micrositeTheme := "pattern",
  micrositePalette := Map(
    "brand-primary" -> "#5B5988",
    "brand-secondary" -> "#292E53",
    "brand-tertiary" -> "#222749",
    "gray-dark" -> "#49494B",
    "gray" -> "#7B7B7E",
    "gray-light" -> "#E5E5E6",
    "gray-lighter" -> "#F4F3F4",
    "white-color" -> "#FFFFFF"
  ),
  micrositeConfigYaml := ConfigYml(yamlInline = s"""
      |scalafiddle:
      |  dependency: io.circe %%% circe-core % $scalaFiddleCirceVersion,io.circe %%% circe-generic % $scalaFiddleCirceVersion,io.circe %%% circe-parser % $scalaFiddleCirceVersion
    """.stripMargin),
  docsMappingsAPIDir := "api",
  addMappingsToSiteDir(ScalaUnidoc / packageDoc / mappings, docsMappingsAPIDir),
  ghpagesNoJekyll := true,
  ScalaUnidoc / unidoc / scalacOptions ++= Seq(
    "-groups",
    "-implicits",
    "-skip-packages",
    "scalaz",
    "-doc-source-url",
    scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
    "-sourcepath",
    (LocalRootProject / baseDirectory).value.getAbsolutePath
  ),
  scalacOptions ~= {
    _.filterNot(Set("-Yno-predef"))
  },
  git.remoteRepo := "git@github.com:circe/circe.git",
  ScalaUnidoc / unidoc / unidocProjectFilter :=
    inAnyProject -- inProjects(noDocProjects(scalaVersion.value): _*),
  makeSite / includeFilter := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.svg" | "*.js" | "*.swf" | "*.yml" | "*.md"
)

lazy val docs = project
  .dependsOn(core, parser, shapes, testing)
  .settings(
    moduleName := "circe-docs",
    name := "Circe docs",
    mdocIn := file("docs/src/main/tut"),
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-generic-extras" % "0.12.2",
      "io.circe" %% "circe-optics" % "0.12.0"
    )
  )
  .settings(docSettings)
  .settings(noPublishSettings)
  .settings(macroSettings)
  .enablePlugins(GhpagesPlugin)
  .enablePlugins(MicrositesPlugin)
  .enablePlugins(ScalaUnidocPlugin)

lazy val circeCrossModules = Seq[(Project, Project)](
  (numbersTesting, numbersTestingJS),
  (numbers, numbersJS),
  (core, coreJS),
  (pointer, pointerJS),
  (pointerLiteral, pointerLiteralJS),
  (extras, extrasJS),
  (generic, genericJS),
  (shapes, shapesJS),
  (literal, literalJS),
  (refined, refinedJS),
  (parser, parserJS),
  (scodec, scodecJS),
  (testing, testingJS),
  (tests, testsJS),
  (hygiene, hygieneJS)
)

lazy val circeJsModules = Seq[Project](scalajs, scalajsJavaTimeTest)
lazy val circeJvmModules = Seq[Project](benchmark, jawn)
lazy val circeDocsModules = Seq[Project](docs)

lazy val jvmProjects: Seq[Project] =
  circeCrossModules.map(_._1) ++ circeJvmModules

lazy val jsProjects: Seq[Project] =
  circeCrossModules.map(_._2) ++ circeJsModules

lazy val aggregatedProjects: Seq[ProjectReference] = (
  circeCrossModules.flatMap(cp => Seq(cp._1, cp._2)) ++
    circeJsModules ++ circeJvmModules
).map(p => p: ProjectReference)

lazy val macroSettings: Seq[Setting[_]] = Seq(
  libraryDependencies ++= (if (isScala3.value) Nil
                           else
                             (Seq(
                               scalaOrganization.value % "scala-compiler" % scalaVersion.value % Provided,
                               scalaOrganization.value % "scala-reflect" % scalaVersion.value % Provided
                             ) ++ (
                               if (priorTo2_13(scalaVersion.value)) {
                                 Seq(
                                   compilerPlugin(
                                     ("org.scalamacros" % "paradise" % paradiseVersion).cross(CrossVersion.patch)
                                   )
                                 )
                               } else Nil
                             ))),
  scalacOptions ++= (
    if (priorTo2_13(scalaVersion.value) || isScala3.value) Nil else Seq("-Ymacro-annotations")
  )
)

lazy val circe = project
  .in(file("."))
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
    console / initialCommands :=
      """
        |import io.circe._
        |import io.circe.generic.auto._
        |import io.circe.literal._
        |import io.circe.parser._
        |import io.circe.syntax._
      """.stripMargin
  )
  .aggregate(aggregatedProjects: _*)
  .dependsOn(core, generic, literal, parser)

lazy val numbersTestingBase = circeCrossModule("numbers-testing", mima = previousCirceVersion, CrossType.Pure).settings(
  scalacOptions ~= {
    _.filterNot(Set("-Yno-predef"))
  },
  libraryDependencies += "org.scalacheck" %%% "scalacheck" % scalaCheckVersion,
  coverageExcludedPackages := "io\\.circe\\.numbers\\.testing\\..*"
)

lazy val numbersTesting = numbersTestingBase.jvm
lazy val numbersTestingJS = numbersTestingBase.js

lazy val numbersBase = circeCrossModule("numbers", mima = previousCirceVersion)
  .settings(
    Test / scalacOptions += "-language:implicitConversions",
    libraryDependencies +=
      "org.typelevel" %%% "discipline-munit" % disciplineMunitVersion % Test
  )
  .dependsOn(numbersTestingBase % Test)

lazy val numbers = numbersBase.jvm
lazy val numbersJS = numbersBase.js

lazy val coreBase = circeCrossModule("core", mima = previousCirceVersion)
  .settings(
    libraryDependencies += "org.typelevel" %%% "cats-core" % catsVersion,
    Compile / sourceGenerators += (Compile / sourceManaged).map(Boilerplate.gen).taskValue,
    Compile / unmanagedSourceDirectories ++= {
      def extraDirs(suffix: String) =
        CrossType.Full.sharedSrcDir(baseDirectory.value, "main").toList.map(f => file(f.getPath + suffix))

      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, y)) => extraDirs("-2") ++ (if (y >= 13) extraDirs("-2.13+") else Nil)
        case Some((3, _)) => extraDirs("-3") ++ extraDirs("-2.13+")
        case _            => Nil
      }
    }
  )
  .dependsOn(numbersBase)

lazy val core = coreBase.jvm
lazy val coreJS = coreBase.js

lazy val genericBase = circeCrossModule("generic", mima = previousCirceVersion)
  .settings(macroSettings)
  .settings(
    libraryDependencies ++= (if (isScala3.value) Nil else Seq("com.chuusai" %%% "shapeless" % shapelessVersion)),
    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.AllLibraryJars,
    Compile / unmanagedSourceDirectories ++= {
      def extraDirs(suffix: String) =
        CrossType.Full.sharedSrcDir(baseDirectory.value, "main").toList.map(f => file(f.getPath + suffix))

      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, y)) => extraDirs("-2") ++ (if (y >= 13) extraDirs("-2.13+") else Nil)
        case Some((3, _)) => extraDirs("-3") ++ extraDirs("-2.13+")
        case _            => Nil
      }
    },
    Test / unmanagedSourceDirectories ++= {
      def extraDirs(suffix: String) =
        CrossType.Full.sharedSrcDir(baseDirectory.value, "test").toList.map(f => file(f.getPath + suffix))

      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, y)) => extraDirs("-2") ++ (if (y >= 13) extraDirs("-2.13+") else Nil)
        case Some((3, _)) => extraDirs("-3") ++ extraDirs("-2.13+")
        case _            => Nil
      }
    }
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "jawn-parser" % jawnVersion % Test,
      "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeVersion % Test
    )
  )
  .dependsOn(coreBase, testsBase % Test)

lazy val generic = genericBase.jvm
lazy val genericJS = genericBase.js

lazy val genericSimpleBase = circeCrossModule("generic-simple", mima = previousCirceVersion, CrossType.Pure)
  .settings(macroSettings)
  .settings(
    crossScalaVersions := Seq("2.13.6"),
    libraryDependencies += "com.chuusai" %%% "shapeless" % shapelessVersion,
    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.AllLibraryJars
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "jawn-parser" % jawnVersion % Test,
      "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeVersion % Test
    )
  )
  .dependsOn(coreBase, testsBase % Test, literalBase % Test)

lazy val genericSimple = genericSimpleBase.jvm
lazy val genericSimpleJS = genericSimpleBase.js

lazy val shapesBase = circeCrossModule("shapes", mima = previousCirceVersion, CrossType.Pure)
  .settings(macroSettings)
  .settings(
    libraryDependencies += "com.chuusai" %%% "shapeless" % shapelessVersion,
    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.AllLibraryJars
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "jawn-parser" % jawnVersion % Test,
      "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeVersion % Test
    )
  )
  .dependsOn(coreBase, testsBase % Test, literalBase % Test)

lazy val shapes = shapesBase.jvm
lazy val shapesJS = shapesBase.js

lazy val literalBase = circeCrossModule("literal", mima = previousCirceVersion, CrossType.Pure)
  .settings(macroSettings)
  .settings(
    libraryDependencies ++= Seq(
      ("com.chuusai" %%% "shapeless" % shapelessVersion % Test).cross(CrossVersion.for3Use2_13),
      "org.scalacheck" %%% "scalacheck" % scalaCheckVersion % Test,
      "org.scalameta" %%% "munit" % munitVersion % Test,
      "org.scalameta" %%% "munit-scalacheck" % munitVersion % Test
    )
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "jawn-parser" % jawnVersion % Test,
      "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeVersion % Test
    )
  )
  .dependsOn(coreBase, parserBase % Test, testingBase % Test)

lazy val literal = literalBase.jvm
lazy val literalJS = literalBase.js

lazy val refinedBase = circeCrossModule("refined", mima = previousCirceVersion)
  .settings(
    libraryDependencies ++= Seq(
      "eu.timepit" %%% "refined" % refinedVersion,
      "eu.timepit" %%% "refined-scalacheck" % refinedVersion % Test
    ),
    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.AllLibraryJars
  )
  .jsSettings(
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeVersion % Test
  )
  .dependsOn(coreBase, testsBase % Test)

lazy val refined = refinedBase.jvm
lazy val refinedJS = refinedBase.js

lazy val parserBase = circeCrossModule("parser", mima = previousCirceVersion)
  .jvmConfigure(_.dependsOn(jawn))
  .jsConfigure(_.dependsOn(scalajs))
  .dependsOn(coreBase)

lazy val parser = parserBase.jvm
lazy val parserJS = parserBase.js

lazy val scalajs =
  circeModule("scalajs", mima = None).enablePlugins(ScalaJSPlugin).settings(jsProjectSettings).dependsOn(coreJS)
lazy val scalajsJavaTimeTest = circeModule("scalajs-java-time-test", mima = None)
  .enablePlugins(ScalaJSPlugin)
  .settings(noPublishSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % munitVersion % Test
    ),
    jsProjectSettings
  )
  .dependsOn(coreJS)

lazy val scodecBase = circeCrossModule("scodec", mima = previousCirceVersion)
  .settings(
    libraryDependencies += "org.scodec" %%% "scodec-bits" % "1.1.27",
    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.AllLibraryJars
  )
  .jsSettings(
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeVersion % Test
  )
  .dependsOn(coreBase, testsBase % Test)

lazy val scodec = scodecBase.jvm
lazy val scodecJS = scodecBase.js

lazy val testingBase = circeCrossModule("testing", mima = previousCirceVersion)
  .settings(
    scalacOptions ~= {
      _.filterNot(Set("-Yno-predef"))
    },
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck" % scalaCheckVersion,
      "org.typelevel" %%% "cats-laws" % catsVersion,
      "org.typelevel" %%% "discipline-core" % disciplineVersion
    )
  )
  .settings(
    coverageExcludedPackages := "io\\.circe\\.testing\\..*"
  )
  .dependsOn(coreBase, numbersTestingBase)

lazy val testing = testingBase.jvm
lazy val testingJS = testingBase.js

lazy val testsBase = circeCrossModule("tests", mima = None)
  .settings(noPublishSettings: _*)
  .settings(
    scalacOptions ~= {
      _.filterNot(Set("-Yno-predef"))
    },
    Test / scalacOptions += "-language:implicitConversions",
    libraryDependencies ++= Seq(
      ("com.chuusai" %%% "shapeless" % shapelessVersion).cross(CrossVersion.for3Use2_13),
      "org.typelevel" %%% "discipline-scalatest" % disciplineScalaTestVersion,
      "org.typelevel" %%% "discipline-munit" % disciplineMunitVersion
    ),
    Test / sourceGenerators += (Test / sourceManaged).map(Boilerplate.genTests).taskValue,
    Compile / unmanagedResourceDirectories +=
      file("modules/tests") / "shared" / "src" / "main" / "resources",
    Compile / unmanagedSourceDirectories ++= {
      def extraDirs(suffix: String) =
        List("main").flatMap(CrossType.Full.sharedSrcDir(baseDirectory.value, _)).map(f => file(f.getPath + suffix))

      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, y)) => extraDirs("-2") ++ (if (y >= 13) extraDirs("-2.13+") else Nil)
        case Some((3, _)) => extraDirs("-3") ++ extraDirs("-2.13+")
        case _            => Nil
      }
    },
    Test / unmanagedSourceDirectories ++= {
      def extraDirs(suffix: String) =
        List("test").flatMap(CrossType.Full.sharedSrcDir(baseDirectory.value, _)).map(f => file(f.getPath + suffix))

      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, y)) => extraDirs("-2") ++ (if (y >= 13) extraDirs("-2.13+") else Nil)
        case Some((3, _)) => extraDirs("-3") ++ extraDirs("-2.13+")
        case _            => Nil
      }
    }
  )
  .settings(
    coverageExcludedPackages := "io\\.circe\\.tests\\..*"
  )
  .jvmSettings(
    fork := true
  )
  .jsSettings(
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeVersion % Test
  )
  .dependsOn(coreBase, parserBase, testingBase)

lazy val tests = testsBase.jvm
lazy val testsJS = testsBase.js

lazy val hygieneBase = circeCrossModule("hygiene", mima = None)
  .settings(noPublishSettings)
  .settings(
    scalacOptions ++= Seq("-Yno-imports", "-Yno-predef")
  )
  .dependsOn(coreBase, genericBase, literalBase)

lazy val hygiene = hygieneBase.jvm.dependsOn(jawn)
lazy val hygieneJS = hygieneBase.js

lazy val jawn = circeModule("jawn", mima = previousCirceVersion)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "jawn-parser" % jawnVersion,
      "org.typelevel" %%% "discipline-munit" % disciplineMunitVersion % Test
    )
  )
  .dependsOn(core)

lazy val pointerBase =
  circeCrossModule("pointer", mima = previousCirceVersion, CrossType.Pure)
    .settings(
      libraryDependencies ++= Seq(
        "org.typelevel" %%% "discipline-munit" % disciplineMunitVersion % Test
      )
    )
    .dependsOn(coreBase, parserBase % Test)

lazy val pointer = pointerBase.jvm
lazy val pointerJS = pointerBase.js

lazy val pointerLiteralBase = circeCrossModule("pointer-literal", mima = previousCirceVersion, CrossType.Pure)
  .settings(macroSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % munitVersion % Test,
      "org.scalameta" %%% "munit-scalacheck" % munitVersion % Test
    )
  )
  .dependsOn(coreBase, pointerBase)

lazy val pointerLiteral = pointerLiteralBase.jvm
lazy val pointerLiteralJS = pointerLiteralBase.js

lazy val extrasBase = circeCrossModule("extras", mima = previousCirceVersion).dependsOn(coreBase, testsBase % Test)

lazy val extras = extrasBase.jvm
lazy val extrasJS = extrasBase.js

lazy val benchmark = circeModule("benchmark", mima = None)
  .settings(noPublishSettings)
  .settings(
    scalacOptions ~= {
      _.filterNot(Set("-Yno-predef"))
    },
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-optics" % "0.13.0",
      "org.scalameta" %% "munit" % munitVersion % Test
    )
  )
  .enablePlugins(JmhPlugin)
  .dependsOn(core, generic, jawn, pointer)

lazy val benchmarkDotty = circeModule("benchmark-dotty", mima = None)
  .settings(noPublishSettings)
  .settings(
    scalacOptions ~= {
      _.filterNot(Set("-Yno-predef"))
    }
  )
  .enablePlugins(JmhPlugin)
  .dependsOn(core, jawn)

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseVcsSign := true,
  homepage := Some(url("https://github.com/circe/circe")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { _ =>
    false
  },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots".at(nexus + "content/repositories/snapshots"))
    else
      Some("releases".at(nexus + "service/local/staging/deploy/maven2"))
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
    Developer("travisbrown", "Travis Brown", "travisrobertbrown@gmail.com", url("https://twitter.com/travisbrown"))
  ),
  pomPostProcess := { (node: XmlNode) =>
    new RuleTransformer(
      new RewriteRule {
        private def isTestScope(elem: Elem): Boolean =
          elem.label == "dependency" && elem.child.exists(child => child.label == "scope" && child.text == "test")

        override def transform(node: XmlNode): XmlNodeSeq = node match {
          case elem: Elem if isTestScope(elem) => Nil
          case _                               => node
        }
      }
    ).transform(node).head
  },
  Compile / doc / sources := {
    val src = (Compile / doc / sources).value

    if (isScala3.value) Nil else src
  }
)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
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

lazy val CompileTime = config("compile-time")

val formatCommands = ";scalafmtCheck;test:scalafmtCheck;scalafmtSbtCheck;scalastyle"

addCommandAlias("buildJVM", jvmProjects.map(";" + _.id + "/compile").mkString)
addCommandAlias(
  "validateJVM",
  ";buildJVM" + jvmProjects.map(";" + _.id + "/test").mkString + formatCommands
)
addCommandAlias("buildJS", jsProjects.map(";" + _.id + "/compile").mkString)
addCommandAlias(
  "validateJS",
  ";buildJS" + jsProjects.map(";" + _.id + "/test").mkString + formatCommands
)
addCommandAlias("validate", ";validateJVM;validateJS")
