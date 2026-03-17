import cats.effect.{IO, Resource}
import fs2.io.file.{Files, Path}
import fs2.compression.Compression
import org.http4s.{Method, Request, Uri}
import org.http4s.client.Client
import _root_.io.release.monorepo.{MonorepoReleasePluginLike, MonorepoStepIO}
import _root_.io.release.monorepo.steps.MonorepoReleaseSteps
import sbt._

object FileReleasePlugin extends MonorepoReleasePluginLike[Client[IO]] {

  override def trigger = noTrigger

  override protected def commandName: String = "releaseFiles"

  override def resource: Resource[IO, Client[IO]] =
    FileServerStub.clientResource

  override protected def monorepoReleaseProcess(
      state: State
  ): Seq[Client[IO] => MonorepoStepIO] =
    Seq(
      MonorepoReleaseSteps.initializeVcs,
      MonorepoReleaseSteps.checkCleanWorkingDir,
      MonorepoReleaseSteps.resolveReleaseOrder,
      MonorepoReleaseSteps.detectOrSelectProjects,
      MonorepoReleaseSteps.inquireVersions,
      MonorepoReleaseSteps.validateVersions,
      MonorepoReleaseSteps.setReleaseVersions,
      MonorepoReleaseSteps.commitReleaseVersions,
      MonorepoReleaseSteps.tagReleases,
      compressAndUploadStep,
      MonorepoReleaseSteps.setNextVersions,
      MonorepoReleaseSteps.commitNextVersions
    )

  private val compressAndUploadStep: Client[IO] => MonorepoStepIO =
    MonorepoStepIO
      .perProjectResource[Client[IO]]("compress-and-upload")
      .executeAction(client =>
        (ctx, project) => {
          val dataFile = new java.io.File(project.baseDir, "data")
          val version = project.releaseVersion.getOrElse(
            throw new IllegalStateException(
              s"No release version for ${project.name}"
            )
          )

          val gzippedStream =
            Files[IO]
              .readAll(Path.fromNioPath(dataFile.toPath))
              .through(Compression[IO].gzip())

          val targetUri = Uri.unsafeFromString(
            s"http://localhost/files/${project.name}/$version/data.gz"
          )

          val request = Request[IO](
            method = Method.PUT,
            uri = targetUri
          ).withBodyStream(gzippedStream)

          client.expect[String](request).flatMap { response =>
            IO(ctx.state.log.info(
              s"[release] Uploaded ${project.name} $version data.gz: $response"
            ))
          }
        }
      )
}
