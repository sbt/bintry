lazy val dispatchVersion = SettingKey[String]("dispatchVersion")
lazy val unusedWarnings = Seq("-Ywarn-unused-import", "-Ywarn-unused")

lazy val commonSettings: Seq[Setting[_]] = Seq(
    version in ThisBuild := "0.5.0-SNAPSHOT",
    organization in ThisBuild := "org.foundweekends",
    homepage in ThisBuild := Some(url(s"https://github.com/sbt/${name.value}/#readme")),
    licenses in ThisBuild := Seq("MIT" ->
      url(s"https://github.com/sbt/${name.value}/blob/${version.value}/LICENSE")),
    description in ThisBuild := "your packages, delivered fresh",
    developers in ThisBuild := List(
      Developer("softprops", "Doug Tangren", "@softprops", url("https://github.com/softprops"))
    ),
    scmInfo in ThisBuild := Some(ScmInfo(url(s"https://github.com/sbt/${name.value}"), s"git@github.com:sbt/{name.value}.git")),
    crossScalaVersions in ThisBuild := Seq("2.10.6", "2.11.8", "2.12.2"),
    scalaVersion := (crossScalaVersions in ThisBuild).value.last,
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
    dispatchVersion := {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, 10)) => "0.11.3"
        case _             => "0.12.0"
      }
    },
    libraryDependencies ++= Seq("net.databinder.dispatch" %% "dispatch-json4s-native" % dispatchVersion.value),
    initialCommands := "import scala.concurrent.ExecutionContext.Implicits.global;"
  )
