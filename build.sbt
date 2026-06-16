Global / onChangedBuildSource := ReloadOnSourceChanges

val scala2 = "2.13.18"
val scala3 = "3.8.4"

ThisBuild / version := "0.0.1"
ThisBuild / organization := "bondlink"
ThisBuild / homepage := Some(url("https://github.com/mblink/sbt-s3-publish"))
ThisBuild / scalaVersion := scala3

LocalRootProject / publish / skip := true

// GitHub Actions config
val javaVersions = Seq(17, 21, 25).map(v => JavaSpec.temurin(v.toString))

ThisBuild / githubWorkflowJavaVersions := javaVersions
ThisBuild / githubWorkflowArtifactUpload := false
ThisBuild / githubWorkflowBuildMatrixFailFast := Some(false)
ThisBuild / githubWorkflowTargetBranches := Seq("main")
ThisBuild / githubWorkflowPublishTargetBranches := Seq()

def isJava(v: Int) = s"matrix.java == '${javaVersions.find(_.version == v.toString).get.render}'"

ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Run(List("sbt test"), name = Some("Build project")),
)

lazy val sbtS3Publish = projectMatrix.in(file("plugin"))
  .jvmPlatform(scalaVersions = Seq(scala2, scala3))
  .settings(
    name := "sbt-s3-publish",
    pluginCrossBuild / sbtVersion := (scalaBinaryVersion.value match {
      case "2.12" => "1.9.0"
      case _ => "2.0.0"
    }),
    s3PublishBucket := "bondlink-maven-repo",
    libraryDependencies += "software.amazon.awssdk" % "s3" % "2.46.10",
  )
  .enablePlugins(SbtPlugin)
