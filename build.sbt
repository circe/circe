import ReleaseTransformations._
import microsites.ExtraMdFileConfig
import microsites.ConfigYml
import org.scalajs.sbtplugin.cross.{ CrossProject, CrossType }
import scala.xml.{ Elem, Node => XmlNode, NodeSeq => XmlNodeSeq }
import scala.xml.transform.{ RewriteRule, RuleTransformer }

organization in ThisBuild := "io.circe"

val compilerOptions = Seq(
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

val catsVersion = "1.1.0"
val jawnVersion = "0.11.1"
val shapelessVersion = "2.3.3"
val refinedVersion = "0.8.7"
val monocleVersion = "1.5.0-cats"

val paradiseVersion = "2.1.1"
val scalaTestVersion = "3.0.5"
val scalaCheckVersion = "1.13.5"
val disciplineVersion = "0.9.0"

val previousCirceVersion = Some("0.9.0")
val scalaFiddleCirceVersion = "0.9.1"

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
  coverageHighlighting := (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 10)) => false
      case _ => true
    }
  ),
  coverageScalacPluginVersion := "1.3.1",
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
  micrositeConfigYaml := ConfigYml(yamlInline =
    s"""
      |scalafiddle:
      |  dependency: io.circe %%% circe-core % $scalaFiddleCirceVersion,io.circe %%% circe-generic % $scalaFiddleCirceVersion,io.circe %%% circe-parser % $scalaFiddleCirceVersion
    """.stripMargin),
  addMappingsToSiteDir(mappings in (ScalaUnidoc, packageDoc), micrositeDocumentationUrl),
  ghpagesNoJekyll := true,
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

lazy val docs = project.dependsOn(core, genericExtras, parser, optics)
  .settings(
    moduleName := "circe-docs",
    name := "Circe docs"
  )
  .settings(docSettings)
  .settings(noPublishSettings)
  .settings(
    addCompilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.patch)
  )
  .enablePlugins(GhpagesPlugin)
  .enablePlugins(MicrositesPlugin)
  .enablePlugins(ScalaUnidocPlugin)

lazy val circeCrossModules = Seq[(Project, Project)](
  (numbersTesting, numbersTestingJS),
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
  (java8, java8JS),
  (testing, testingJS),
  (tests, testsJS)
)

lazy val circeJsModules = Seq[Project](scalajs)
lazy val circeJvmModules = Seq[Project](jawn)
lazy val circeDocsModules = Seq[Project](docs)
lazy val circeUtilModules = Seq[Project](hygiene, hygieneJS, benchmark)

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
   circeJsModules ++ circeJvmModules ++ circeDocsModules ++ circeUtilModules)
    .filterNot(jvm8Only(java8)).map(p => p: ProjectReference)

def macroSettings(scaladocFor210: Boolean): Seq[Setting[_]] = Seq(
  libraryDependencies ++= Seq(
    scalaOrganization.value % "scala-compiler" % scalaVersion.value % Provided,
    scalaOrganization.value % "scala-reflect" % scalaVersion.value % Provided,
    "org.typelevel" %%% "macro-compat" % "1.1.1",
    compilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.patch)
  ),
  libraryDependencies ++= {
    CrossVersion.partialVersion(scalaVersion.value) match {
      // if scala 2.11+ is used, quasiquotes are merged into scala-reflect.
      case Some((2, scalaMajor)) if scalaMajor >= 11 => Nil
      // in Scala 2.10, quasiquotes are provided by macro paradise.
      case Some((2, 10)) => Seq("org.scalamacros" %% "quasiquotes" % paradiseVersion cross CrossVersion.binary)
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
    addCompilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.patch),
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

lazy val numbersTestingBase = circeCrossModule("numbers-testing", mima = previousCirceVersion, CrossType.Pure)
  .settings(
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
      "org.scalatest" %%% "scalatest" % scalaTestVersion % Test
    )
  ).dependsOn(numbersTestingBase % Test)

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
  .jsConfigure(_.settings(libraryDependencies += "org.spire-math" %% "jawn-parser" % jawnVersion % Test))
  .dependsOn(coreBase, testsBase % Test, literalBase % Test)

lazy val generic = genericBase.jvm
lazy val genericJS = genericBase.js

lazy val genericExtrasBase = circeCrossModule("generic-extras", mima = previousCirceVersion, CrossType.Pure)
  .settings(macroSettings(scaladocFor210 = false))
  .jsConfigure(_.settings(libraryDependencies += "org.spire-math" %% "jawn-parser" % jawnVersion % Test))
  .jvmSettings(fork in Test := true)
  .dependsOn(genericBase, testsBase % Test, literalBase % Test)

lazy val genericExtras = genericExtrasBase.jvm
lazy val genericExtrasJS = genericExtrasBase.js

lazy val shapesBase = circeCrossModule("shapes", mima = previousCirceVersion, CrossType.Pure)
  .settings(macroSettings(scaladocFor210 = true))
  .settings(
    libraryDependencies += "com.chuusai" %%% "shapeless" % shapelessVersion
  )
  .jsConfigure(_.settings(libraryDependencies += "org.spire-math" %% "jawn-parser" % jawnVersion % Test))
  .dependsOn(coreBase, testsBase % Test, literalBase % Test)

lazy val shapes = shapesBase.jvm
lazy val shapesJS = shapesBase.js

lazy val literalBase = circeCrossModule("literal", mima = previousCirceVersion, CrossType.Pure)
  .settings(macroSettings(scaladocFor210 = false))
  .settings(libraryDependencies += "com.chuusai" %%% "shapeless" % shapelessVersion % Test)
  .jsConfigure(_.settings(libraryDependencies += "org.spire-math" %% "jawn-parser" % jawnVersion % Test))
  .dependsOn(coreBase, parserBase % Test, testingBase % Test)

lazy val literal = literalBase.jvm
lazy val literalJS = literalBase.js

lazy val refinedBase = circeCrossModule("refined", mima = previousCirceVersion)
  .settings(
    libraryDependencies ++= Seq(
      "eu.timepit" %%% "refined" % refinedVersion,
      "eu.timepit" %%% "refined-scalacheck" % refinedVersion % Test
    )
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

lazy val scalajs = circeModule("scalajs", mima = None)
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(coreJS)

lazy val scodecBase = circeCrossModule("scodec", mima = previousCirceVersion)
  .settings(
    libraryDependencies += "org.scodec" %%% "scodec-bits" % "1.1.5"
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
      "org.scalacheck" %%% "scalacheck" % scalaCheckVersion % Test,
      "org.scalatest" %%% "scalatest" % scalaTestVersion,
      "org.typelevel" %%% "cats-laws" % catsVersion,
      "org.typelevel" %%% "discipline" % disciplineVersion
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
    libraryDependencies ++= Seq(
      "com.chuusai" %%% "shapeless" % shapelessVersion,
      "org.scalacheck" %%% "scalacheck" % scalaCheckVersion,
      "org.scalatest" %%% "scalatest" % scalaTestVersion,
      "org.typelevel" %%% "cats-laws" % catsVersion,
      "org.typelevel" %%% "discipline" % disciplineVersion
    ),
    sourceGenerators in Test += (sourceManaged in Test).map(Boilerplate.genTests).taskValue,
    unmanagedResourceDirectories in Compile +=
      file("modules/tests") / "shared" / "src" / "main" / "resources"
  )
  .settings(
    coverageExcludedPackages := "io\\.circe\\.tests\\..*"
  )
  .jvmSettings(fork := true)
  .dependsOn(coreBase, parserBase, testingBase)

lazy val tests = testsBase.jvm
lazy val testsJS = testsBase.js

lazy val hygieneBase = circeCrossModule("hygiene", mima = None)
  .settings(noPublishSettings)
  .settings(
    crossScalaVersions := crossScalaVersions.value.tail,
    scalacOptions ++= Seq("-Yno-imports", "-Yno-predef")
  )
  .dependsOn(coreBase, genericBase, literalBase)

lazy val hygiene = hygieneBase.jvm.dependsOn(jawn)
lazy val hygieneJS = hygieneBase.js

lazy val jawn = circeModule("jawn", mima = previousCirceVersion)
  .settings(
    libraryDependencies += "org.spire-math" %% "jawn-parser" % jawnVersion
  )
  .dependsOn(core)

lazy val java8Base = circeCrossModule("java8", mima = previousCirceVersion, CrossType.Pure)
  .dependsOn(coreBase, testsBase % Test)
  .jsSettings(
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % "2.0.0-M13"
  )

lazy val java8 = java8Base.jvm
lazy val java8JS = java8Base.js

lazy val opticsBase = circeCrossModule("optics", mima = previousCirceVersion, CrossType.Pure)
  .settings(
    libraryDependencies ++= Seq(
      "com.github.julien-truffaut" %%% "monocle-core" % monocleVersion,
      "com.github.julien-truffaut" %%% "monocle-law"  % monocleVersion % Test,
      compilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.patch)
    )
  )
  .dependsOn(coreBase, genericBase % Test, testsBase % Test)

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
      compilerPlugin("org.scalamacros" % "paradise" % paradiseVersion cross CrossVersion.patch)
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
  ),
  pomPostProcess := { (node: XmlNode) =>
    new RuleTransformer(
      new RewriteRule {
        private def isTestScope(elem: Elem): Boolean =
          elem.label == "dependency" && elem.child.exists(child => child.label == "scope" && child.text == "test")

        override def transform(node: XmlNode): XmlNodeSeq = node match {
          case elem: Elem if isTestScope(elem) => Nil
          case _ => node
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

val jvmTestProjects = jvmProjects.filterNot(Set(core, jawn, parser))
val jsTestProjects = jsProjects.filterNot(Set(core, parser, scalajs))

addCommandAlias("buildJVM", jvmProjects.map(";" + _.id + "/compile").mkString)
addCommandAlias(
  "validateJVM",
  ";buildJVM" + jvmTestProjects.map(";" + _.id + "/test").mkString + ";scalastyle;unidoc"
)
addCommandAlias("buildJS", jsProjects.map(";" + _.id + "/compile").mkString)
addCommandAlias(
  "validateJS",
  ";buildJS" + jsTestProjects.map(";" + _.id + "/test").mkString + ";scalastyle"
)
addCommandAlias("validate", ";validateJVM;validateJS")
