import sbtcrossproject.{ CrossProject, CrossType }
import com.typesafe.tools.mima.core._

val Scala212V: String = "2.12.20"
val Scala213V: String = "2.13.16"
val Scala3V: String = "3.3.6"

ThisBuild / startYear := Some(2024)
ThisBuild / tlBaseVersion := "0.14"
ThisBuild / tlCiReleaseBranches := Seq() // set to `series/0.14.x` once we get the automated publishing process up and running
ThisBuild / tlCiReleaseTags := true
ThisBuild / tlFatalWarnings := false // we currently have a lot of warnings that will need to be fixed

ThisBuild / organization := "io.circe"
ThisBuild / crossScalaVersions := List(Scala3V, Scala212V, Scala213V)
ThisBuild / scalaVersion := Scala213V

ThisBuild / githubWorkflowJavaVersions := Seq("8", "11", "17").map(JavaSpec.temurin)

ThisBuild / tlCiScalafixCheck := false // TODO: Address these in a follow up PR
ThisBuild / scalafixAll / skip := tlIsScala3.value
ThisBuild / ScalafixConfig / skip := tlIsScala3.value
ThisBuild / circeRootOfCodeCoverage := Some("rootJVM")

//This is really bad, but we need to add the _native0.5 for this: https://github.com/sbt/sbt/issues/7140
ThisBuild / libraryDependencySchemes +=
  "org.scala-native" %% "test-interface_native0.5" % VersionScheme.Always

val opticsVersion = "0.15.0"

val catsVersion = "2.13.0"
val jawnVersion = "1.6.0"
val shapelessVersion = "2.3.13"

val paradiseVersion = "2.1.1"

val scalaCheckVersion = "1.18.1"
val munitVersion = "1.1.1"
val munitScalaCheckVersion = "1.1.0"
val disciplineVersion = "1.7.0"
val disciplineScalaTestVersion = "2.3.0"
val disciplineMunitVersion = "2.0.0"
val scalaJavaTimeVersion = "2.6.0"

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

lazy val allSettings = Seq()

/**
 * Replace '/' with '-' in a path which represents a module name.
 *
 * The circe module's path is used in most cases to derive the module
 * name. For some modules, this path includes sub-directories, e.g. scalafix
 * internal rules. When this is the case, since the path is represented as a
 * simple String, the '/' character can cause problems as the module name is
 * used inside SBT and Coursier for Maven style artifact operations and '/'
 * is not a valid character in a module name.
 */
def normalizeModuleNameFromPath(path: String): String =
  path.replaceAll("/", "-")

def circeProject(path: String)(project: Project) = {
  val docName = path.split("[-/]").mkString(" ")
  project.settings(
    description := s"circe $docName",
    moduleName := s"circe-${normalizeModuleNameFromPath(path)}",
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
      tlVersionIntroduced := List("2.12", "2.13", "3").map(_ -> "0.14.9").toMap
    )
}

lazy val docs = project
  .in(file("site"))
  .dependsOn(core.jvm, parser.jvm, shapes.jvm, testing.jvm)
  .settings(
    moduleName := "circe-docs",
    name := "Circe docs",
    libraryDependencies ++= Seq(
      "io.circe" %% "circe-generic-extras" % "0.14.3",
      "io.circe" %% "circe-optics" % opticsVersion
    ),
    tlSitePublishBranch := Some("series/0.14.x"),
    mdocVariables ++= Map(
      "CIRCE_OPTICS_VERSION" -> opticsVersion
    )
  )
  .enablePlugins(CirceOrgSitePlugin)
  .settings(macroSettings)

lazy val macroSettings: Seq[Setting[?]] = Seq(
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
    Compile / sourceGenerators += Def.task {
      val managedSource = (Compile / sourceManaged).value
      val currentScalaVersion = (Compile / scalaBinaryVersion).value
      Boilerplate.gen(managedSource, currentScalaVersion)
    },
    scalacOptions ~= (_.filterNot(Set("-source:3.0-migration"))),
    mimaBinaryIssueFilters ++= Seq(
      ProblemFilters.exclude[ReversedMissingMethodProblem]("io.circe.Json.traverseBoolean"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("io.circe.Json.traverseNumber"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("io.circe.Json.traverseString"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("io.circe.Json.traverseArray"),
      ProblemFilters.exclude[ReversedMissingMethodProblem]("io.circe.Json.traverseObject")
    )
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
      "org.scalameta" %%% "munit-scalacheck" % munitScalaCheckVersion % Test
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
    tlVersionIntroduced := List("2.12", "2.13", "3").map(_ -> "0.14.9").toMap
  )
  .dependsOn(core, jawn, testing % Test)

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
    libraryDependencies += "org.scodec" %%% "scodec-bits" % "1.2.1"
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
    scalacOptions ~= {
      _.filterNot(Set("-Yno-predef", "-source:3.0-migration"))
    },
    Test / scalacOptions += "-language:implicitConversions",
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
      "org.scalameta" %%% "munit-scalacheck" % munitScalaCheckVersion % Test
    )
  )
  .nativeSettings(
    tlVersionIntroduced := List("2.12", "2.13", "3").map(_ -> "0.14.9").toMap
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
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(
  Developer("travisbrown", "Travis Brown", "travisrobertbrown@gmail.com", url("https://twitter.com/travisbrown")),
  Developer("zmccoy", "Zach McCoy", "zachabbott@gmail.com", url("https://twitter.com/zachamccoy")),
  Developer("zarthross", "Darren Gibson", "zarthross@gmail.com", url("https://twitter.com/zarthross")),
  Developer("hamnis", "Erlend Hamnaberg", "erlend@hamnaberg.net", url("https://github.com/hamnis"))
)
