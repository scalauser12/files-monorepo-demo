import cats.effect.{IO, Ref, Resource}
import fs2.Chunk
import org.http4s.{HttpApp, HttpRoutes}
import org.http4s.client.Client
import org.http4s.dsl.io.*

object FileServerStub {

  def clientResource: Resource[IO, Client[IO]] =
    Resource
      .eval(Ref.of[IO, Map[String, Array[Byte]]](Map.empty))
      .map(store => Client.fromHttpApp(httpApp(store)))

  private def httpApp(store: Ref[IO, Map[String, Array[Byte]]]): HttpApp[IO] =
    routes(store).orNotFound

  private def routes(store: Ref[IO, Map[String, Array[Byte]]]): HttpRoutes[IO] =
    HttpRoutes.of[IO] {
      case req @ PUT -> Root / "files" / project / version / filename =>
        val key = s"$project/$version/$filename"
        req.body.compile
          .to(Chunk)
          .flatMap(chunk =>
            store.update(_ + (key -> chunk.toArray)) *>
              Created(s"Stored $key (${chunk.size} bytes)")
          )

      case GET -> Root / "files" / project / version / filename =>
        val key = s"$project/$version/$filename"
        store.get.flatMap(_.get(key) match {
          case Some(bytes) => Ok(bytes)
          case None        => NotFound(s"$key not found")
        })

      case GET -> Root / "files" =>
        store.get.flatMap(m => Ok(m.keys.mkString("\n")))
    }
}
