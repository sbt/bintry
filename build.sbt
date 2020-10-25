ThisBuild / version := "0.5.3-SNAPSHOT"
ThisBuild / organization := "org.foundweekends"
ThisBuild / homepage := Some(url(s"https://github.com/sbt/${name.value}/#readme"))
ThisBuild / description := "your packages, delivered fresh"
ThisBuild / developers := List(
  Developer("softprops", "Doug Tangren", "@softprops", url("https://github.com/softprops"))
)
ThisBuild / scmInfo := Some(ScmInfo(url(s"https://github.com/sbt/${name.value}"), s"git@github.com:sbt/{name.value}.git"))
ThisBuild / crossScalaVersions := Seq("2.12.12", "2.13.3")
ThisBuild / scalaVersion := (ThisBuild / crossScalaVersions).value.last

lazy val dispatchVersion = settingKey[String]("")
lazy val unusedWarnings = Seq("-Ywarn-unused")

lazy val commonSettings: Seq[Setting[_]] = Seq(
    licenses := Seq("MIT" ->
      url(s"https://github.com/sbt/${name.value}/blob/${version.value}/LICENSE")),
    scalacOptions ++= Seq(Opts.compile.deprecation, "-Xlint", "-feature"),
    scalacOptions ++= PartialFunction.condOpt(CrossVersion.partialVersion(scalaVersion.value)){
      case Some((2, v)) if v >= 11 => unusedWarnings
    }.toList.flatten,
    publishArtifact in Test := false,
    publishTo := {
      val v = version.value
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT")) Some("snapshots" at nexus + "content/repositories/snapshots")
      else Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    publishMavenStyle := true,
    pomIncludeRepository := { x => false }
  ) ++ Seq(Compile, Test).flatMap(c =>
    scalacOptions in (c, console) --= unusedWarnings
  )

lazy val root = (project in file("."))
  .settings(commonSettings)
  .settings(
    name := "bintry",
    description := "your packages, delivered fresh",
    dispatchVersion := "1.2.0",
    libraryDependencies ++= Seq(
      "org.dispatchhttp" %% "dispatch-json4s-native" % dispatchVersion.value,
      "com.eed3si9n.verify" %% "verify" % "0.2.0" % Test,
    ),
    testFrameworks += new TestFramework("verify.runner.Framework"),
    initialCommands := "import scala.concurrent.ExecutionContext.Implicits.global;"
  )
