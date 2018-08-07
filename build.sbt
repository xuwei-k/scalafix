import Dependencies._
inThisBuild(
  List(
    onLoadMessage := s"Welcome to scalafix ${version.value}",
    scalaVersion := "2.12.6",
    crossScalaVersions := List("2.12.6", "2.11.12")
  )
)

noPublish

lazy val core = project
  .in(file("scalafix-core"))
  .settings(
    moduleName := "scalafix-core",
    buildInfoSettings,
    libraryDependencies ++= List(
      scalameta,
      symtab,
      googleDiff,
      "com.geirsson" %% "metaconfig-typesafe-config" % metaconfigV,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided
    )
  )
  .enablePlugins(BuildInfoPlugin)

lazy val reflect = project
  .in(file("scalafix-reflect"))
  .settings(
    moduleName := "scalafix-reflect",
    isFullCrossVersion,
    libraryDependencies ++= Seq(
      metacp,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
    )
  )
  .dependsOn(core)

lazy val cli = project
  .in(file("scalafix-cli"))
  .settings(
    moduleName := "scalafix-cli",
    isFullCrossVersion,
    mainClass in assembly := Some("scalafix.v1.Main"),
    assemblyJarName in assembly := "scalafix.jar",
    libraryDependencies ++= Seq(
      "com.martiansoftware" % "nailgun-server" % "0.9.1",
      jgit,
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "org.apache.commons" % "commons-text" % "1.2"
    )
  )
  .dependsOn(reflect)

lazy val testsShared = project
  .in(file("scalafix-tests/shared"))
  .settings(
    semanticdbSettings,
    noPublish
  )

lazy val testsInput = project
  .in(file("scalafix-tests/input"))
  .settings(
    noPublish,
    semanticdbSettings,
    scalacOptions ~= (_.filterNot(_ == "-Yno-adapted-args")),
    scalacOptions += "-Ywarn-adapted-args", // For NoAutoTupling
    scalacOptions += "-Ywarn-unused-import", // For RemoveUnusedImports
    scalacOptions += "-Ywarn-unused", // For RemoveUnusedTerms
    logLevel := Level.Error, // avoid flood of compiler warnings
    testsInputOutputSetting
  )

lazy val testsOutput = project
  .in(file("scalafix-tests/output"))
  .settings(
    noPublish,
    semanticdbSettings,
    scalacOptions --= List(
      warnUnusedImports,
      "-Xlint"
    ),
    testsInputOutputSetting
  )

lazy val testsOutputDotty = project
  .in(file("scalafix-tests/output-dotty"))
  .settings(
    noPublish,
    // Skip this project for IntellIJ, see https://youtrack.jetbrains.com/issue/SCL-12237
    SettingKey[Boolean]("ide-skip-project") := true,
    scalaVersion := dotty,
    crossScalaVersions := List(dotty),
    libraryDependencies := libraryDependencies.value.map(
      _.withDottyCompat(scalaVersion.value)),
    scalacOptions := Nil
  )

lazy val testkit = project
  .in(file("scalafix-testkit"))
  .settings(
    moduleName := "scalafix-testkit",
    isFullCrossVersion,
    libraryDependencies ++= Seq(
      semanticdb,
      googleDiff,
      scalatest
    )
  )
  .dependsOn(cli)

lazy val unit = project
  .in(file("scalafix-tests/unit"))
  .settings(
    noPublish,
    fork := false,
    javaOptions := Nil,
    buildInfoPackage := "scalafix.tests",
    buildInfoObject := "BuildInfo",
    libraryDependencies ++= coursierDeps,
    libraryDependencies ++= List(
      jgit,
      scalatest
    ),
    compileInputs.in(Compile, compile) := {
      compileInputs
        .in(Compile, compile)
        .dependsOn(
          compile.in(testsInput, Compile),
          compile.in(testsOutputDotty, Compile),
          compile.in(testsOutput, Compile)
        )
        .value
    },
    resourceGenerators.in(Test) += Def.task {
      // copy-pasted code from ScalafixTestkitPlugin to avoid cyclic dependencies between build and sbt-scalafix.
      val props = new java.util.Properties()

      def put(key: String, files: Seq[File]): Unit = {
        val value = files.iterator
          .filter(_.exists())
          .mkString(java.io.File.pathSeparator)
        props.put(key, value)
      }

      put(
        "inputClasspath",
        fullClasspath.in(testsInput, Compile).value.map(_.data)
      )
      put(
        "inputSourceDirectories",
        sourceDirectories.in(testsInput, Compile).value
      )
      put(
        "outputSourceDirectories",
        sourceDirectories.in(testsOutput, Compile).value ++
          sourceDirectories.in(testsOutputDotty, Compile).value
      )
      val out =
        managedResourceDirectories.in(Test).value.head /
          "scalafix-testkit.properties"
      IO.write(props, "Input data for scalafix testkit", out)
      List(out)
    },
    buildInfoKeys := Seq[BuildInfoKey](
      "baseDirectory" ->
        baseDirectory.in(ThisBuild).value,
      "inputSourceroot" ->
        sourceDirectory.in(testsInput, Compile).value,
      "outputSourceroot" ->
        sourceDirectory.in(testsOutput, Compile).value,
      "testsInputResources" ->
        sourceDirectory.in(testsInput, Compile).value / "resources",
      "outputDottySourceroot" ->
        sourceDirectory.in(testsOutputDotty, Compile).value,
      "semanticClasspath" ->
        classDirectory.in(testsInput, Compile).value,
      "sharedSourceroot" ->
        baseDirectory.in(ThisBuild).value /
          "scalafix-tests" / "shared" / "src" / "main",
      "sharedClasspath" ->
        classDirectory.in(testsShared, Compile).value
    )
  )
  .enablePlugins(BuildInfoPlugin)
  .dependsOn(
    testsInput,
    testsShared,
    cli,
    testkit
  )

lazy val website = project
  .enablePlugins(MicrositesPlugin)
  .enablePlugins(ScalaUnidocPlugin)
  .settings(
    scalaVersion := scala212,
    noPublish,
    websiteSettings,
    unidocSettings,
    libraryDependencies += "com.geirsson" %% "metaconfig-docs" % metaconfigV,
    unidocProjectFilter in (ScalaUnidoc, unidoc) := inProjects(
      testkit,
      core
    )
  )
  .dependsOn(testkit, core, cli)
