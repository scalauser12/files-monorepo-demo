import sbt.*
import Keys.*

object FileProjectsPlugin extends AutoPlugin {

  val dataFileName    = "data"
  val versionFileName = "version.txt"

  override def trigger = noTrigger

  lazy val discoveredProjects: Seq[Project] = {
    val projectsDir = file("projects")
    if (projectsDir.isDirectory)
      Option(projectsDir.listFiles())
        .getOrElse(Array.empty)
        .toSeq
        .filter(dir =>
          dir.isDirectory && (dir / versionFileName).isFile && (dir / dataFileName).isFile
        )
        .sortBy(_.getName)
        .map(dir =>
          Project(dir.getName, dir).settings(
            name         := dir.getName,
            // Avoid warnings about missing Scala version
            scalaVersion := "2.12.21"
          )
        )
    else Seq.empty
  }

  override def extraProjects: Seq[Project] = discoveredProjects
}
