organization := "me.lessis"

name := "bintry"

version := "0.2.0"

description := "your packages, delivered fresh"

crossScalaVersions := Seq("2.9.3", "2.10.0", "2.10.1")

scalaVersion := "2.9.3"

libraryDependencies ++= Seq("net.databinder.dispatch" %% "dispatch-json4s-native" % "0.11.0")

publishTo := Some(Opts.resolver.sonatypeStaging)

publishMavenStyle := true

publishArtifact in Test := false

licenses <<= (version)(v =>
      Seq("MIT" ->
          url("https://github.com/softprops/bintry/blob/%s/LICENSE" format v)))

homepage := some(url("https://github.com/softprops/bintry/#readme"))

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


seq(lsSettings:_*)

(LsKeys.tags in LsKeys.lsync) := Seq("bintray", "dispatch", "http")
