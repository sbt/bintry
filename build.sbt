organization := "me.lessis"

name := "bintry"

version := "0.3.0-SNAPSHOT"

description := "your packages, delivered fresh"

crossScalaVersions := Seq("2.10.4", "2.11.0")

scalaVersion := crossScalaVersions.value.head

libraryDependencies ++= Seq("net.databinder.dispatch" %% "dispatch-json4s-native" % "0.11.1")

publishTo := Some(Opts.resolver.sonatypeStaging)

publishMavenStyle := true

publishArtifact in Test := false

licenses := Seq("MIT" ->
                url(s"https://github.com/softprops/${name.value}/blob/${version.value}/LICENSE"))

homepage := some(url(s"https://github.com/softprops/${name.value}/#readme"))

pomExtra := (
  <scm>
    <url>git@github.com:softprops/bintry.git</url>
    <connection>scm:git:git@github.com:softprops/bintry.git</connection>
  </scm>
  <developers>
    <developer>
      <id>softprops</id>
      <name>Doug Tangren</name>
      <url>https://github.com/softprops</url>
    </developer>
  </developers>)


lsSettings

(LsKeys.tags in LsKeys.lsync) := Seq("bintray", "dispatch", "http")
