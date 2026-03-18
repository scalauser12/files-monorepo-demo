import _root_.io.release.monorepo.{MonorepoReleasePluginLike, MonorepoStepIO}
import _root_.io.release.monorepo.steps.MonorepoReleaseSteps
import cats.effect.{IO, Resource}
import fs2.compression.Compression
import fs2.io.file.{Files, Path}
import org.http4s.{Method, Request, Uri}
import org.http4s.client.Client
import sbt.*

object FileReleasePlugin extends MonorepoReleasePluginLike[Client[IO]] {

  override def trigger = noTrigger

  override protected def commandName: String = "releaseFiles"

  override def requires = FileProjectsPlugin

  override def resource: Resource[IO, Client[IO]] = FileServerStub.clientResource

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
      // MonorepoReleaseSteps.pushChanges // disabled for now to avoid pushing to remote
    )

  private val compressAndUploadStep: Client[IO] => MonorepoStepIO =
    MonorepoStepIO
      .perProjectResource[Client[IO]]("compress-and-upload")
      .executeAction(client =>
        (ctx, project) => {
          val dataFile = new File(project.baseDir, FileProjectsPlugin.dataFileName)

          val gzippedStream =
            Files[IO]
              .readAll(Path.fromNioPath(dataFile.toPath))
              .through(Compression[IO].gzip())

          for {
            version   <- IO.fromOption(project.releaseVersion)(
                           new IllegalStateException(
                             s"No release version for ${project.name}"
                           )
                         )
            targetUri <- IO.fromEither(
                           Uri.fromString(
                             s"http://localhost/files/${project.name}/$version/data.gz"
                           )
                         )
            request    = Request[IO](
                           method = Method.PUT,
                           uri = targetUri
                         ).withBodyStream(gzippedStream)
            response  <- client.expect[String](request)
            _         <- IO.blocking(
                           ctx.state.log.info(
                             s"[release-io] Uploaded ${project.name} $version data.gz: $response"
                           )
                         )
          } yield ()
        }
      )
}
