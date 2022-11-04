import microsites.ExtraMdFileConfig
import microsites.ConfigYml
import sbtcrossproject.{ CrossProject, CrossType }

val Scala212V: String = "2.12.15"
val Scala213V: String = "2.13.8"
val Scala3V: String = "3.1.3"

ThisBuild / tlBaseVersion := "0.14"
ThisBuild / tlCiReleaseTags := false

ThisBuild / organization := "io.circe"
ThisBuild / crossScalaVersions := List(Scala3V, Scala212V, Scala213V)
ThisBuild / scalaVersion := Scala213V

ThisBuild / githubWorkflowJavaVersions := Seq("8", "11", "17").map(JavaSpec.temurin)

ThisBuild / githubWorkflowAddedJobs ++= Seq(
  WorkflowJob(
    id = "scalafmt",
    name = "Scalafmt and Scalastyle",
    scalas = List(crossScalaVersions.value.last),
    steps = List(WorkflowStep.Checkout) ++ WorkflowStep.SetupJava(
      List(githubWorkflowJavaVersions.value.last)
    ) ++ githubWorkflowGeneratedCacheSteps.value ++ List(
      WorkflowStep.Sbt(
        List("+scalafmtCheckAll", "scalafmtSbtCheck", "scalastyle"),
        name = Some("Scalafmt and Scalastyle tests")
      )
    )
  ),
  WorkflowJob(
    id = "coverage",
    name = "Generate coverage report",
    scalas = crossScalaVersions.value.filterNot(_.startsWith("3.")).toList,
    steps = List(WorkflowStep.Checkout) ++ WorkflowStep.SetupJava(
      List(githubWorkflowJavaVersions.value.last)
    ) ++ githubWorkflowGeneratedCacheSteps.value ++ List(
      WorkflowStep.Sbt(List("coverage", "rootJVM/test", "coverageAggregate")),
      WorkflowStep.Use(
        UseRef.Public(
          "codecov",
          "codecov-action",
          "v2"
        ),
        params = Map(
          "flags" -> List("${{matrix.scala}}", "${{matrix.java}}").mkString(",")
        )
      )
    )
  )
)

ThisBuild / scalafixScalaBinaryVersion := CrossVersion.binaryScalaVersion(scalaVersion.value)
ThisBuild / scalafixAll / skip := tlIsScala3.value
ThisBuild / ScalafixConfig / skip := tlIsScala3.value

val catsVersion = "2.8.0"
val jawnVersion = "1.4.0"
val shapelessVersion = "2.3.10"
val refinedVersion = "0.9.29"
val refinedNativeVersion = "0.10.1"

val paradiseVersion = "2.1.1"

val scalaCheckVersion = "1.17.0"
val munitVersion = "1.0.0-M6"
val disciplineVersion = "1.5.1"
val disciplineScalaTestVersion = "2.2.0"
val disciplineMunitVersion = "2.0.0-M3"
val scalaJavaTimeVersion = "2.4.0"

/**
 * Some terrible hacks to work around Cats's decision to have builds for
 * different Scala versions depend on different versions of Discipline, etc.
 */
def priorTo2_13(scalaVersion: String): Boolean =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, minor)) if minor < 13 => true
    case _                              => false
  }

val scalaFiddleCirceVersion = "0.9.1"

lazy val allSettings = Seq(
  coverageHighlighting := true,
  Compile / scalastyleSources ++= (Compile / unmanagedSourceDirectories).value
)

def circeProject(path: String)(project: Project) = {
  val docName = path.split("[-/]").mkString(" ")
  project.settings(
    description := s"circe $docName",
    moduleName := s"circe-$path",
    name := s"Circe $docName",
    allSettings
  )
}

/**
 * This is here so we can use this with our internal Scalafix rules, without
 * creating a cyclic dependency. So the scalafix modules will use this and
 * the other modules will use either `circeModule` or `circeCrossModule`.
 */
def baseModule(path: String, additionalDeps: List[ClasspathDep[ProjectReference]] = Nil): Project = {
  val id = path.split("[-/]").reduce(_ + _.capitalize)
  Project(id, file(s"modules/$path")).configure(circeProject(path)).configure(_.dependsOn(additionalDeps: _*))
}

def circeModule(path: String): Project = baseModule(path, List(scalafixInternalRules % ScalafixConfig))

def circeCrossModule(path: String, crossType: CrossType = CrossType.Full) = {
  val id = path.split("[-/]").reduce(_ + _.capitalize)
  CrossProject(id, file(s"modules/$path"))(JVMPlatform, JSPlatform, NativePlatform)
    .crossType(crossType)
    .settings(allSettings)
    .configure(circeProject(path))
    .jsSettings(
      coverageEnabled := false
    )
    .configure(_.dependsOn(scalafixInternalRules % ScalafixConfig))
    .nativeSettings(
      coverageEnabled := false,
      tlVersionIntroduced := List("2.12", "2.13", "3").map(_ -> "0.14.3").toMap
    )
}

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
  micrositeExtraMdFiles := Map(
    file("CONTRIBUTING.md") -> ExtraMdFileConfig(
      "contributing.md",
      "docs",
      Map("title" -> "Contributing", "position" -> "6")
    )
  ),
  micrositeExtraMdFilesOutput := resourceManaged.value / "main" / "jekyll",
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
  /* Publish GitHub Pages { */
  gitHubPagesOrgName := "circe",
  gitHubPagesRepoName := "circe",
  gitHubPagesSiteDir := baseDirectory.value / "target" / "site",
  /* } Publish GitHub Pages */
  scalacOptions ~= {
    _.filterNot(Set("-Yno-predef"))
  },
  git.remoteRepo := "git@github.com:circe/circe.git",
  ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(
    numbers.jvm,
    core.jvm,
    pointer.jvm,
    pointerLiteral.jvm,
    generic.jvm,
    shapes.jvm,
    literal.jvm,
    refined.jvm,
    parser.jvm,
    scodec.jvm,
    jawn.jvm,
    scalajs
  ),
  makeSite / includeFilter := "*.html" | "*.css" | "*.png" | "*.jpg" | "*.gif" | "*.svg" | "*.js" | "*.swf" | "*.yml" | "*.md"
)

lazy val docs = project
  .dependsOn(core.jvm, parser.jvm, shapes.jvm, testing.jvm)
  .settings(
    moduleName := "circe-docs",
    name := "Circe docs",
    mdocIn := file("docs/src/main/tut"),
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-generic-extras" % "0.14.1",
      "io.circe" %% "circe-optics" % "0.14.1"
    )
  )
  .settings(docSettings)
  .enablePlugins(NoPublishPlugin)
  .settings(macroSettings)
  .enablePlugins(GhpagesPlugin)
  .enablePlugins(MicrositesPlugin)
  .enablePlugins(ScalaUnidocPlugin)
  .enablePlugins(GitHubPagesPlugin)

lazy val macroSettings: Seq[Setting[_]] = Seq(
  libraryDependencies ++= (if (tlIsScala3.value) Nil
                           else
                             Seq(
                               scalaOrganization.value % "scala-compiler" % scalaVersion.value % Provided,
                               scalaOrganization.value % "scala-reflect" % scalaVersion.value % Provided
                             ) ++ (
                               if (scalaBinaryVersion.value == "2.12") {
                                 Seq(
                                   compilerPlugin(
                                     ("org.scalamacros" % "paradise" % paradiseVersion).cross(CrossVersion.patch)
                                   )
                                 )
                               } else Nil
                             )),
  scalacOptions ++= (
    if (Set("2.12", "3").contains(scalaBinaryVersion.value)) Nil else Seq("-Ymacro-annotations")
  ),
  scalacOptions -= "-source:3.0-migration"
)

lazy val root = tlCrossRootProject
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
  .aggregate(
    benchmark,
    core,
    extras,
    generic,
    hygiene,
    jawn,
    literal,
    numbers,
    numbersTesting,
    parser,
    pointer,
    pointerLiteral,
    refined,
    scalafixInternalInput,
    scalafixInternalOutput,
    scalafixInternalRules,
    scalafixInternalTests,
    scalajs,
    scalajsJavaTimeTest,
    scodec,
    shapes,
    testing,
    tests
  )
  .disablePlugins(ScalafixPlugin)

lazy val scalafixInternalRules =
  baseModule("scalafix/internal/rules")
    .settings(
      skip := tlIsScala3.value,
      update / skip := false,
      libraryDependencies ++= List(
        "ch.epfl.scala" %% "scalafix-core" % _root_.scalafix.sbt.BuildInfo.scalafixVersion
      ).filterNot(_ => tlIsScala3.value)
    )
    .enablePlugins(NoPublishPlugin)
    .disablePlugins(ScalafixPlugin)

lazy val scalafixInternalInput =
  baseModule("scalafix/internal/input")
    .settings(
      skip := tlIsScala3.value,
      update / skip := false
    )
    .disablePlugins(ScalafixPlugin)
    .enablePlugins(NoPublishPlugin)

lazy val scalafixInternalOutput =
  baseModule("scalafix/internal/output")
    .settings(
      skip := tlIsScala3.value,
      update / skip := false
    )
    .disablePlugins(ScalafixPlugin)
    .enablePlugins(NoPublishPlugin)

lazy val scalafixInternalTests =
  baseModule("scalafix/internal/tests")
    .enablePlugins(NoPublishPlugin, ScalafixTestkitPlugin)
    .settings(
      libraryDependencies := {
        if (tlIsScala3.value)
          libraryDependencies.value.filterNot(_.name == "scalafix-testkit")
        else
          libraryDependencies.value
      },
      scalafixTestkitOutputSourceDirectories :=
        (scalafixInternalOutput / Compile / sourceDirectories).value,
      scalafixTestkitInputSourceDirectories :=
        (scalafixInternalInput / Compile / sourceDirectories).value,
      scalafixTestkitInputClasspath :=
        (scalafixInternalInput / Compile / fullClasspath).value,
      scalafixTestkitInputScalacOptions :=
        (scalafixInternalInput / Compile / scalacOptions).value,
      scalafixTestkitInputScalaVersion :=
        (scalafixInternalInput / Compile / scalaVersion).value,
      libraryDependencies ++= Seq(
        ("ch.epfl.scala" %% "scalafix-testkit" % _root_.scalafix.sbt.BuildInfo.scalafixVersion % Test)
          .cross(CrossVersion.full)
      ).filter(_ => !tlIsScala3.value),
      Compile / compile :=
        (Compile / compile).dependsOn(scalafixInternalInput / Compile / compile).value
    )
    .disablePlugins(ScalafixPlugin)
    .dependsOn(scalafixInternalInput, scalafixInternalOutput, scalafixInternalRules)

lazy val numbersTesting =
  circeCrossModule("numbers-testing", CrossType.Pure).settings(
    libraryDependencies += "org.scalacheck" %%% "scalacheck" % scalaCheckVersion,
    coverageExcludedPackages := "io\\.circe\\.numbers\\.testing\\..*"
  )

lazy val numbers = circeCrossModule("numbers")
  .settings(
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % munitVersion % Test,
      "org.typelevel" %%% "discipline-munit" % disciplineMunitVersion % Test
    )
  )
  .dependsOn(numbersTesting % Test)

lazy val core = circeCrossModule("core")
  .settings(
    libraryDependencies += "org.typelevel" %%% "cats-core" % catsVersion,
    Compile / sourceGenerators += (Compile / sourceManaged).map(Boilerplate.gen).taskValue
  )
  .dependsOn(numbers)

lazy val generic = circeCrossModule("generic")
  .settings(macroSettings)
  .settings(
    libraryDependencies ++= (if (tlIsScala3.value) Nil
                             else Seq("com.chuusai" %%% "shapeless" % shapelessVersion))
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "jawn-parser" % jawnVersion % Test,
      "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeVersion % Test
    )
  )
  .dependsOn(core, tests % Test)

lazy val genericSimple = circeCrossModule("generic-simple", CrossType.Pure)
  .settings(macroSettings)
  .settings(
    crossScalaVersions := (ThisBuild / crossScalaVersions).value.filter(_.startsWith("2.13")),
    libraryDependencies += "com.chuusai" %%% "shapeless" % shapelessVersion,
    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.AllLibraryJars
  )
  .platformsSettings(JSPlatform, NativePlatform)(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "jawn-parser" % jawnVersion % Test,
      "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeVersion % Test
    )
  )
  .dependsOn(core, tests % Test, literal % Test)

lazy val shapes = circeCrossModule("shapes", CrossType.Pure)
  .settings(macroSettings)
  .settings(
    publish / skip := tlIsScala3.value,
    publishArtifact := !tlIsScala3.value,
    libraryDependencies += ("com.chuusai" %%% "shapeless" % shapelessVersion).cross(CrossVersion.for3Use2_13)
  )
  .platformsSettings(JSPlatform, NativePlatform)(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "jawn-parser" % jawnVersion % Test,
      "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeVersion % Test
    )
  )
  .dependsOn(core, tests % Test, literal % Test)

lazy val literal = circeCrossModule("literal", CrossType.Pure)
  .settings(macroSettings)
  .settings(
    tlVersionIntroduced += "3" -> "0.14.2",
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck" % scalaCheckVersion % Test,
      "org.scalameta" %%% "munit" % munitVersion % Test,
      "org.scalameta" %%% "munit-scalacheck" % munitVersion % Test
    ) ++ (if (tlIsScala3.value) Seq("org.typelevel" %%% "jawn-parser" % jawnVersion % Provided)
          else Seq("com.chuusai" %%% "shapeless" % shapelessVersion))
  )
  .platformsSettings(JSPlatform, NativePlatform)(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeVersion % Test,
      "org.typelevel" %%% "jawn-parser" % jawnVersion % Provided
    )
  )
  .nativeSettings(
    tlVersionIntroduced := List("2.12", "2.13", "3").map(_ -> "0.14.3").toMap
  )
  .dependsOn(core, parser % Test, testing % Test)

lazy val refined = circeCrossModule("refined")
  .settings(
    tlVersionIntroduced += "3" -> "0.14.3",
    libraryDependencies ++= {
      val refinedV =
        if (crossProjectPlatform.value == NativePlatform) refinedNativeVersion
        else refinedVersion
      Seq(
        "eu.timepit" %%% "refined" % refinedV,
        "eu.timepit" %%% "refined-scalacheck" % refinedV % Test
      )
    },
    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.AllLibraryJars
  )
  .platformsSettings(JSPlatform, NativePlatform)(
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeVersion % Test
  )
  .dependsOn(core, tests % Test)

lazy val parser =
  circeCrossModule("parser")
    .jvmConfigure(_.dependsOn(jawn.jvm))
    .jsConfigure(_.dependsOn(scalajs))
    .nativeConfigure(_.dependsOn(jawn.native))
    .dependsOn(core)

lazy val scalajs =
  circeModule("scalajs").enablePlugins(ScalaJSPlugin).dependsOn(core.js)
lazy val scalajsJavaTimeTest = circeModule("scalajs-java-time-test")
  .enablePlugins(ScalaJSPlugin)
  .enablePlugins(NoPublishPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % munitVersion % Test
    )
  )
  .dependsOn(core.js)

lazy val scodec = circeCrossModule("scodec")
  .settings(
    libraryDependencies += "org.scodec" %%% "scodec-bits" % "1.1.34"
  )
  .platformsSettings(JSPlatform, NativePlatform)(
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeVersion % Test
  )
  .dependsOn(core, tests % Test)

lazy val testing = circeCrossModule("testing")
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
  .dependsOn(core, numbersTesting)

lazy val tests = circeCrossModule("tests")
  .enablePlugins(NoPublishPlugin)
  .settings(
    libraryDependencies ++= Seq(
      ("com.chuusai" %%% "shapeless" % shapelessVersion).cross(CrossVersion.for3Use2_13),
      "org.scalameta" %%% "munit" % munitVersion,
      "org.typelevel" %%% "discipline-scalatest" % disciplineScalaTestVersion,
      "org.typelevel" %%% "discipline-munit" % disciplineMunitVersion
    ),
    Test / sourceGenerators += (Test / sourceManaged).map(Boilerplate.genTests).taskValue
  )
  .settings(
    coverageExcludedPackages := "io\\.circe\\.tests\\..*"
  )
  .jvmSettings(
    fork := true
  )
  .platformsSettings(JSPlatform, NativePlatform)(
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeVersion % Test
  )
  .nativeSettings(
    nativeConfig ~= { _.withEmbedResources(true) }
  )
  .dependsOn(core, parser, testing, jawn)

lazy val hygiene = circeCrossModule("hygiene")
  .enablePlugins(NoPublishPlugin)
  .settings(
    scalacOptions ++= Seq("-Yno-imports", "-Yno-predef")
  )
  .dependsOn(core, generic, literal, jawn)

lazy val jawn = circeCrossModule("jawn", CrossType.Full)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "jawn-parser" % jawnVersion,
      "org.scalameta" %%% "munit" % munitVersion % Test,
      "org.typelevel" %%% "discipline-munit" % disciplineMunitVersion % Test
    )
  )
  .jsSettings(
    tlVersionIntroduced := List("2.12", "2.13", "3").map(_ -> "0.14.2").toMap
  )
  .dependsOn(core)

lazy val pointer =
  circeCrossModule("pointer", CrossType.Pure)
    .settings(
      libraryDependencies ++= Seq(
        "org.scalameta" %%% "munit" % munitVersion % Test,
        "org.typelevel" %%% "discipline-munit" % disciplineMunitVersion % Test
      )
    )
    .dependsOn(core, parser % Test)

lazy val pointerLiteral = circeCrossModule("pointer-literal", CrossType.Pure)
  .settings(macroSettings)
  .settings(
    tlVersionIntroduced += "3" -> "0.14.2",
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % munitVersion % Test,
      "org.scalameta" %%% "munit-scalacheck" % munitVersion % Test
    )
  )
  .nativeSettings(
    tlVersionIntroduced := List("2.12", "2.13", "3").map(_ -> "0.14.3").toMap
  )
  .dependsOn(core, pointer % "compile;test->test")

lazy val extras = circeCrossModule("extras").enablePlugins(NoPublishPlugin).dependsOn(core, tests % Test)

lazy val benchmark = circeModule("benchmark")
  .settings(
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % munitVersion % Test
    ) ++ { if (tlIsScala3.value) Nil else List("io.circe" %% "circe-optics" % "0.14.1") }
  )
  .enablePlugins(JmhPlugin, NoPublishPlugin)
  .dependsOn(core.jvm, generic.jvm, jawn.jvm, pointer.jvm)

ThisBuild / homepage := Some(url("https://github.com/circe/circe"))
ThisBuild / licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / developers := List(
  Developer("travisbrown", "Travis Brown", "travisrobertbrown@gmail.com", url("https://twitter.com/travisbrown")),
  Developer("zmccoy", "Zach McCoy", "zachabbott@gmail.com", url("https://twitter.com/zachamccoy")),
  Developer("zarthross", "Darren Gibson", "zarthross@gmail.com", url("https://twitter.com/zarthross"))
)
