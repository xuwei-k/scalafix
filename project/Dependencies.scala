import sbt._
/* scalafmt: { maxColumn = 120 }*/

object Dependencies {
  val scalametaV = "4.0.0-M6"
  val metaconfigV = "0.8.3"
  def dotty = "0.9.0-RC1"
  def scala210 = "2.10.6"
  def scala211 = "2.11.12"
  def scala212 = "2.12.6"
  val currentScalaVersion = scala212

  def sbt013 = "0.13.17"
  def sbt1 = "1.0.4"
  val currentSbtVersion = sbt1

  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit" % "4.5.4.201711221230-r"

  var testClasspath: String = "empty"
  def semanticdb: ModuleID = "org.scalameta" % "semanticdb-scalac" % scalametaV cross CrossVersion.full
  def metaconfig: ModuleID = "com.geirsson" %% "metaconfig-typesafe-config" % metaconfigV
  def googleDiff = "com.googlecode.java-diff-utils" % "diffutils" % "1.3.0"

  def metacp = "org.scalameta" %% "metacp" % scalametaV
  def scalameta = "org.scalameta" %% "contrib" % scalametaV
  def symtab = "org.scalameta" %% "symtab" % scalametaV
  def scalatest = "org.scalatest" %% "scalatest" % "3.2.0-SNAP10"
  def scalacheck = "org.scalacheck" %% "scalacheck" % "1.14.0"

  def testsDeps = List(
    // integration property tests
    "org.renucci" %% "scala-xml-quote" % "0.1.4",
    "org.typelevel" %% "catalysts-platform" % "0.0.5",
    "com.chuusai" %% "shapeless" % "2.3.2",
    scalacheck
  )

  def coursierDeps = Seq(
    "io.get-coursier" %% "coursier" % coursier.util.Properties.version,
    "io.get-coursier" %% "coursier-cache" % coursier.util.Properties.version
  )
}
