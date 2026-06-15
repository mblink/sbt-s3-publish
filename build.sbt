ThisBuild / version := "0.0.1-SNAPSHOT"
ThisBuild / organization := "bondlink"
ThisBuild / homepage := Some(url("https://github.com/mblink/sbt-s3-publish"))

val scala2 = "2.12.21"
val scala3 = "3.8.4"

ThisBuild / scalaVersion := scala3

lazy val sbtS3Publish = projectMatrix.in(file("plugin"))
  .jvmPlatform(scalaVersions = Seq(scala2, scala3))
  .settings(
    name := "sbt-s3-publish",
    pluginCrossBuild / sbtVersion := (scalaBinaryVersion.value match {
      case "2.12" => "1.9.0"
      case _ => "2.0.0"
    }),
    publishTo := Some(Resolver.file("file", file("/src/maven-repo"))),
    libraryDependencies += "software.amazon.awssdk" % "s3" % "2.46.10",
  )
  .enablePlugins(SbtPlugin)
