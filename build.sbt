organization := "me.lessis"

name := "bintry"

version := "0.5.0-SNAPSHOT"

description := "your packages, delivered fresh"

crossScalaVersions := Seq("2.10.6", "2.11.8")

scalaVersion := crossScalaVersions.value.last

val unusedWarnings = Seq("-Ywarn-unused-import", "-Ywarn-unused")

scalacOptions ++= Seq(Opts.compile.deprecation, "-Xlint", "-feature")

scalacOptions ++= PartialFunction.condOpt(CrossVersion.partialVersion(scalaVersion.value)){
  case Some((2, v)) if v >= 11 => unusedWarnings
}.toList.flatten

Seq(Compile, Test).flatMap(c =>
  scalacOptions in (c, console) --= unusedWarnings
)

val dispatchVersion = SettingKey[String]("dispatchVersion")

dispatchVersion := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, 10)) =>
      "0.11.3"
    case _ =>
      "0.12.0"
  }
}

libraryDependencies ++= Seq("net.databinder.dispatch" %% "dispatch-json4s-native" % dispatchVersion.value)

initialCommands := "import scala.concurrent.ExecutionContext.Implicits.global;"

licenses := Seq("MIT" ->
                url(s"https://github.com/sbt/${name.value}/blob/${version.value}/LICENSE"))

homepage := Some(url(s"https://github.com/sbt/${name.value}/#readme"))

bintrayPackageLabels := Seq("bintray", "dispatch", "http")

publishArtifact in Test := false

pomExtra := (
  <scm>
    <url>git@github.com:sbt/{name.value}.git</url>
    <connection>scm:git:git@github.com:sbt/{name.value}.git</connection>
  </scm>
  <developers>
    <developer>
      <id>softprops</id>
      <name>Doug Tangren</name>
      <url>https://github.com/softprops</url>
    </developer>
  </developers>)
