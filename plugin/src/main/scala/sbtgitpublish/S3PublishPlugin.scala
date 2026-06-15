package sbts3publish

import org.apache.ivy.core.module.descriptor.{Artifact, DefaultArtifact}
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.plugins.resolver.FileSystemResolver
import sbt._, Keys._
import sbt.internal.librarymanagement.IvyActions
import scala.sys.process._
import scala.util.Try
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.{GetBucketLocationRequest, PutObjectRequest}

object S3PublishPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  object autoImport {
    val s3PublishDir = settingKey[File]("The local directory to stage releases in")
    val s3PublishBucket = settingKey[String]("The S3 bucket to publish to")
    val s3Release = taskKey[Unit]("Push the package to S3")
    val s3ReleaseCleanLocal = taskKey[Unit]("Clean local files after a release to S3")
  }

  import autoImport._

  private val getPublishedFiles: Def.Initialize[Task[Vector[File]]] = Def.task {
    val log = streams.value.log

    // Get all files expected to be published in the local `s3PublishDir`
    val publishConf = publishConfiguration.value
    val publishIvy = ivyModule.value
    val (publishModule, publishCrossVersion) = Some(publishIvy.moduleSettings) match {
      case Some(c: ModuleDescriptorConfiguration) => (c.module, CrossVersion(c.module, c.scalaModuleInfo))
      case _ => sys.error("Module needs ModuleDescriptorConfiguration")
    }

    publishIvy.withModule(log) { case (ivy, md, _) =>
      val repo = ivy.getSettings.getResolver(publishConf.resolverName.getOrElse("")).asInstanceOf[FileSystemResolver]
      val destPattern = repo.getArtifactPatterns.get(0).asInstanceOf[String].replace("[organisation]", "[orgPath]")
      val getDestFile = {
        val m = classOf[FileSystemResolver].getDeclaredMethod(
          "getDestination",
          classOf[String],
          classOf[Artifact],
          classOf[ModuleRevisionId]
        )
        m.setAccessible(true)
        (a: Artifact) => new File(m.invoke(repo, destPattern, a, md.getModuleRevisionId).asInstanceOf[String])
      }

      IvyActions.mapArtifacts(md, publishCrossVersion, Map(publishConf.artifacts*)).flatMap { case (a, f) =>
        getDestFile(a) +: publishConf.checksums.map(c => getDestFile(DefaultArtifact.cloneWithAnotherExt(a, s"${a.getExt}.$c")))
      }
    }
  }

  val packagePublishSettings = Seq(
    s3PublishDir := sys.env.get("XDG_CACHE_HOME").fold(file(sys.env("HOME")) / ".cache")(file(_)) / "sbt-s3-publish",
    publishTo := {
      val dir = s3PublishDir.value
      Some(Resolver.file(dir.toString, dir))
    },
    s3Release / skip := (publish / skip).value,
    s3Release := {
      if (!(s3Release / skip).value) {
        val log = streams.value.log
        val dir = s3PublishDir.value
        val publishedFiles = getPublishedFiles.value

        // Verify all published files exist
        publishedFiles.filterNot(_.exists) match {
          case Vector() => ()
          case missing => sys.error(
            s"$dir does not contain all expected published files. Did you run `publish` before `s3Release`?" ++
            s"\n\nMissing: ${missing.map(s => s"\n  $s").mkString}"
          )
        }

        // Get the S3 bucket we're publishing to
        val bucket = s3PublishBucket.?.value.getOrElse(sys.error("No `s3PublishBucket` specified"))

        // Lock to prevent other release processes from modifying local or remote state while we're pushing this release
        S3PublishPlugin.synchronized {
          val s3WithDefaultRegion = S3Client.create()

          try {
            // Get bucket location
            val location = s3WithDefaultRegion.getBucketLocation(GetBucketLocationRequest.builder.bucket(bucket).build)

            // Create a new S3 client with the correct region
            val s3WithCorrectRegion = Option(location.locationConstraintAsString) match {
              case Some("") | None => s3WithDefaultRegion
              case Some(location) => S3Client.builder.region(Region.of(location)).build()
            }

            try {
              // Upload files to S3
              publishedFiles.foreach { file =>
                val key = file.toString.stripPrefix(dir.toString).stripPrefix("/")
                log.info(s"Uploading $file to s3://$bucket/$key")

                s3WithCorrectRegion.putObject(
                  PutObjectRequest.builder.bucket(bucket).key(key).build,
                  file.toPath,
                )
              }
            } finally {
              s3WithCorrectRegion.close()
            }
          } finally {
            s3WithDefaultRegion.close()
          }
        }
      }
    },
    s3ReleaseCleanLocal := {
      val log = streams.value.log
      val publishedFiles = getPublishedFiles.value

      publishedFiles.filter(_.exists).foreach { file =>
        log.info(s"Removing $file")
        file.delete
      }
    },
  )

  override def projectSettings = packagePublishSettings
}
