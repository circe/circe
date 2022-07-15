import microsites.ExtraMdFileConfig
import microsites.ConfigYml
import sbtcrossproject.{ CrossProject, CrossType }

ThisBuild / tlBaseVersion := "0.14"
ThisBuild / tlCiReleaseTags := false

ThisBuild / organization := "io.circe"
ThisBuild / crossScalaVersions := List("3.1.0", "2.12.15", "2.13.8")
ThisBuild / tlSkipIrrelevantScalas := true
ThisBuild / scalaVersion := crossScalaVersions.value.last

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

val catsVersion = "2.7.0"
val jawnVersion = "1.3.2"
val shapelessVersion = "2.3.9"
val refinedVersion = "0.9.28"

val paradiseVersion = "2.1.1"

val scalaTestVersion = "3.2.9"
val scalaCheckVersion = "1.15.4"
val munitVersion = "0.7.29"
val disciplineVersion = "1.4.0"
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

val scalaFiddleCirceVersion = "0.9.1"

lazy val allSettings = Seq(
  coverageHighlighting := true,
  Compile / scalastyleSources ++= (Compile / unmanagedSourceDirectories).value
)

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
  Project(id, file(s"modules/$path")).configure(circeProject(path))
}

def circeCrossModule(path: String, crossType: CrossType = CrossType.Full) = {
  val id = path.split("-").reduce(_ + _.capitalize)
  CrossProject(id, file(s"modules/$path"))(JVMPlatform, JSPlatform)
    .crossType(crossType)
    .settings(allSettings)
    .configure(circeProject(path))
    .jsSettings(
      coverageEnabled := false
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
                             (Seq(
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
                             ))),
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
    numbersTesting,
    numbers,
    core,
    pointer,
    pointerLiteral,
    extras,
    generic,
    shapes,
    literal,
    refined,
    parser,
    scodec,
    testing,
    tests,
    hygiene,
    jawn,
    scalajs,
    scalajsJavaTimeTest,
    benchmark
  )

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
  .jsSettings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "jawn-parser" % jawnVersion % Test,
      "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeVersion % Test
    )
  )
  .dependsOn(core, tests % Test, literal % Test)

lazy val shapes = circeCrossModule("shapes", CrossType.Pure)
  .settings(macroSettings)
  .settings(
    crossScalaVersions := (ThisBuild / crossScalaVersions).value.filterNot(_.startsWith("3.")),
    libraryDependencies += ("com.chuusai" %%% "shapeless" % shapelessVersion).cross(CrossVersion.for3Use2_13)
  )
  .jsSettings(
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
  .jsSettings(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeVersion % Test,
      "org.typelevel" %%% "jawn-parser" % jawnVersion % Provided
    )
  )
  .dependsOn(core, parser % Test, testing % Test)

lazy val refined = circeCrossModule("refined")
  .settings(
    tlVersionIntroduced += "3" -> "0.14.3",
    libraryDependencies ++= Seq(
      "eu.timepit" %%% "refined" % refinedVersion,
      "eu.timepit" %%% "refined-scalacheck" % refinedVersion % Test
    ),
    Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.AllLibraryJars
  )
  .jsSettings(
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeVersion % Test
  )
  .dependsOn(core, tests % Test)

lazy val parser =
  circeCrossModule("parser").jvmConfigure(_.dependsOn(jawn.jvm)).jsConfigure(_.dependsOn(scalajs)).dependsOn(core)

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
    libraryDependencies += "org.scodec" %%% "scodec-bits" % "1.1.30"
  )
  .jsSettings(
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
  .jsSettings(
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time" % scalaJavaTimeVersion % Test
  )
  .dependsOn(core, parser, testing, jawn)

lazy val hygiene = circeCrossModule("hygiene")
  .enablePlugins(NoPublishPlugin)
  .settings(
    crossScalaVersions := (ThisBuild / crossScalaVersions).value.filterNot(_.startsWith("3.")),
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
  .dependsOn(core, pointer % "compile;test->test")

lazy val extras = circeCrossModule("extras")
  .settings(crossScalaVersions := (ThisBuild / crossScalaVersions).value.filterNot(_.startsWith("3.")))
  .enablePlugins(NoPublishPlugin)
  .dependsOn(core, tests % Test)

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
