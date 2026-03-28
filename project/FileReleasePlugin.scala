import _root_.io.release.monorepo.{
  MonorepoProjectResourceHookIO,
  MonorepoReleasePluginLike,
  MonorepoResourceHooks
}
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

  override protected def monorepoResourceHooks(
      state: State
  ): MonorepoResourceHooks[Client[IO]] =
    MonorepoResourceHooks(
      afterTagHooks = Seq(compressAndUploadHook)
    )

  private val compressAndUploadHook: MonorepoProjectResourceHookIO[Client[IO]] =
    MonorepoProjectResourceHookIO[Client[IO]](
      name = "compress-and-upload",
      execute = client =>
        (ctx, project) => {
          val dataFile = projectDataFile(project.baseDir)

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
          } yield ctx
        },
      validate = (_, project) =>
        IO.blocking {
          val dataFile = projectDataFile(project.baseDir)
          if (!dataFile.isFile)
            throw new IllegalStateException(
              s"Missing data file for ${project.name}: ${dataFile.getAbsolutePath}"
            )
        }
    )

  private def projectDataFile(baseDir: File): File =
    new File(baseDir, FileProjectsPlugin.dataFileName)
}
