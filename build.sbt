import ReleaseTransformations._
import microsites.ExtraMdFileConfig
import microsites.ConfigYml
import sbtcrossproject.{ CrossProject, CrossType }
import scala.xml.{ Elem, Node => XmlNode, NodeSeq => XmlNodeSeq }
import scala.xml.transform.{ RewriteRule, RuleTransformer }

organization in ThisBuild := "io.circe"

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

val catsVersion = "2.0.0-M4"
val jawnVersion = "0.14.2"
val shapelessVersion = "2.3.3"
val refinedVersion = "0.9.8"

val paradiseVersion = "2.1.1"

val scalaTestVersion = "3.1.0-SNAP13"
val scalaCheckVersion = "1.14.0"
val disciplineVersion = "0.12.0-M3"
val scalaJavaTimeVersion = "2.0.0-RC3"

/**
 * Some terrible hacks to work around Cats's decision to have builds for
 * different Scala versions depend on different versions of Discipline, etc.
 */
def priorTo2_13(scalaVersion: String): Boolean =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, minor)) if minor < 13 => true
    case _                              => false
  }

val previousCirceVersion = Some("0.11.0")
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
  scalacOptions in (Compile, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports", "-Yno-predef"))
  },
  scalacOptions in (Test, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports", "-Yno-predef"))
  },
  scalacOptions in Tut ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Yno-predef"))
  },
  scalacOptions in Test ~= {
    _.filterNot(Set("-Yno-predef"))
  },
  resolvers ++= Seq(
    Resolver.sonatypeRepo("releases"),
    Resolver.sonatypeRepo("snapshots")
  ),
  coverageHighlighting := true,
  (scalastyleSources in Compile) ++= (unmanagedSourceDirectories in Compile).value,
  ivyConfigurations += CompileTime.hide,
  unmanagedClasspath in Compile ++= update.value.select(configurationFilter(CompileTime.name)),
  unmanagedClasspath in Test ++= update.value.select(configurationFilter(CompileTime.name))
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
  CrossProject(id, file(s"modules/$path"))(JVMPlatform, JSPlatform)
    .crossType(crossType)
    .settings(allSettings)
    .configure(circeProject(path))
    .jvmSettings(
      mimaPreviousArtifacts := mima.map("io.circe" %% moduleName.value % _).toSet
    )
}

/**
 * We omit all Scala.js projects from Unidoc generation.
 */
def noDocProjects(sv: String): Seq[ProjectReference] =
  (circeCrossModules.map(_._2) :+ tests :+ genericSimple :+ genericSimpleJS).map(p => p: ProjectReference)

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
    "white-color" -> "#FFFFFF"
  ),
  micrositeConfigYaml := ConfigYml(yamlInline = s"""
      |scalafiddle:
      |  dependency: io.circe %%% circe-core % $scalaFiddleCirceVersion,io.circe %%% circe-generic % $scalaFiddleCirceVersion,io.circe %%% circe-parser % $scalaFiddleCirceVersion
    """.stripMargin),
  addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), micrositeDocumentationUrl),
  ghpagesNoJekyll := true,
  scalacOptions in (ScalaUnidoc, unidoc) ++= Seq(
    "-groups",
    "-implicits",
    "-skip-packages",
    "scalaz",
    "-doc-source-url",
    scmInfo.value.get.browseUrl + "/tree/master€{FILE_PATH}.scala",
    "-sourcepath",
    baseDirectory.in(LocalRootProject).value.getAbsolutePath,
    "-doc-root-content",
    (resourceDirectory.in(Compile).value / "rootdoc.txt").getAbsolutePath
  ),
  scalacOptions ~= {
    _.filterNot(Set("-Yno-predef"))
  },
  git.remoteRepo := "git@github.com:circe/circe.git",
  unidocProjectFilter in (ScalaUnidoc, unidoc) :=
    inAnyProject -- inProjects(noDocProjects(scalaVersion.value): _*),
  includeFilter in makeSite := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.svg" | "*.js" | "*.swf" | "*.yml" | "*.md"
)

lazy val docs = project
  .dependsOn(core, genericExtras, parser, shapes)
  .settings(
    moduleName := "circe-docs",
    name := "Circe docs",
    crossScalaVersions := crossScalaVersions.value.filterNot(_.startsWith("2.13")),
    libraryDependencies += "io.circe" %% "circe-optics" % "0.11.0"
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
  (rs, rsJS),
  (generic, genericJS),
  (genericExtras, genericExtrasJS),
  (shapes, shapesJS),
  (literal, literalJS),
  (refined, refinedJS),
  (parser, parserJS),
  (scodec, scodecJS),
  (testing, testingJS),
  (tests, testsJS),
  (hygiene, hygieneJS)
)

lazy val circeJsModules = Seq[Project](scalajs)
lazy val circeJvmModules = Seq[Project](benchmark, jawn)
lazy val circeDocsModules = Seq[Project](docs)

lazy val jvmProjects: Seq[Project] =
  (circeCrossModules.map(_._1) ++ circeJvmModules)

lazy val jsProjects: Seq[Project] =
  (circeCrossModules.map(_._2) ++ circeJsModules)

lazy val aggregatedProjects: Seq[ProjectReference] = (
  circeCrossModules.flatMap(cp => Seq(cp._1, cp._2)) ++
    circeJsModules ++ circeJvmModules
).map(p => p: ProjectReference)

lazy val macroSettings: Seq[Setting[_]] = Seq(
  libraryDependencies ++= Seq(
    scalaOrganization.value % "scala-compiler" % scalaVersion.value % Provided,
    scalaOrganization.value % "scala-reflect" % scalaVersion.value % Provided
  ) ++ (
    if (priorTo2_13(scalaVersion.value)) {
      Seq(
        compilerPlugin(("org.scalamacros" % "paradise" % paradiseVersion).cross(CrossVersion.patch))
      )
    } else Nil
  ),
  scalacOptions ++= (
    if (priorTo2_13(scalaVersion.value)) Nil else Seq("-Ymacro-annotations")
  )
)

lazy val circe = project
  .in(file("."))
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(
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
  .dependsOn(core, genericExtras, literal, parser, rs)

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
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck" % scalaCheckVersion % Test,
      "org.scalatest" %%% "scalatest" % scalaTestVersion % Test,
      "org.scalatestplus" %%% "scalatestplus-scalacheck" % "1.0.0-SNAP8" % Test
    )
  )
  .dependsOn(numbersTestingBase % Test)

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

lazy val rsBase = circeCrossModule("rs", mima = previousCirceVersion, CrossType.Pure).dependsOn(coreBase)

lazy val rs = rsBase.jvm
lazy val rsJS = rsBase.js

lazy val genericBase = circeCrossModule("generic", mima = previousCirceVersion)
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

lazy val generic = genericBase.jvm
lazy val genericJS = genericBase.js

lazy val genericExtrasBase = circeCrossModule("generic-extras", mima = previousCirceVersion, CrossType.Pure)
  .settings(macroSettings)
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "jawn-parser" % jawnVersion % Test,
      "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeVersion % Test
    )
  )
  .jvmSettings(fork in Test := true)
  .dependsOn(genericBase, testsBase % Test, literalBase % Test)

lazy val genericExtras = genericExtrasBase.jvm
lazy val genericExtrasJS = genericExtrasBase.js

lazy val genericSimpleBase = circeCrossModule("generic-simple", mima = previousCirceVersion, CrossType.Pure)
  .settings(macroSettings)
  .settings(
    crossScalaVersions := Seq("2.13.0"),
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
      "com.chuusai" %%% "shapeless" % shapelessVersion % Test,
      "org.scalacheck" %%% "scalacheck" % scalaCheckVersion % Test,
      "org.scalatest" %%% "scalatest" % scalaTestVersion % Test,
      "org.scalatestplus" %%% "scalatestplus-scalacheck" % "1.0.0-SNAP8" % Test
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

lazy val scalajs = circeModule("scalajs", mima = None).enablePlugins(ScalaJSPlugin).dependsOn(coreJS)

lazy val scodecBase = circeCrossModule("scodec", mima = previousCirceVersion)
  .settings(
    libraryDependencies += "org.scodec" %%% "scodec-bits" % "1.1.12",
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
  .dependsOn(coreBase, numbersTestingBase, rsBase)

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
      "org.scalatest" %%% "scalatest" % scalaTestVersion,
      "org.scalatestplus" %%% "scalatestplus-scalacheck" % "1.0.0-SNAP8"
    ),
    sourceGenerators in Test += (sourceManaged in Test).map(Boilerplate.genTests).taskValue,
    unmanagedResourceDirectories in Compile +=
      file("modules/tests") / "shared" / "src" / "main" / "resources"
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
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
      "org.typelevel" %% "jawn-parser" % jawnVersion
    )
  )
  .dependsOn(core)

lazy val benchmark = circeModule("benchmark", mima = None)
  .settings(noPublishSettings)
  .settings(
    scalacOptions ~= {
      _.filterNot(Set("-Yno-predef"))
    },
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test
    )
  )
  .enablePlugins(JmhPlugin)
  .dependsOn(core, generic, jawn, genericExtras)

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  homepage := Some(url("https://github.com/circe/circe")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
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
