import cats.effect.IO

lazy val root = (project in file("."))
  .aggregate(FileProjectsPlugin.discoveredProjects.map(p => LocalProject(p.id)): _*)
  .enablePlugins(FileReleasePlugin)
  .settings(
    name         := "files-monorepo-demo",
    scalaVersion := "2.12.21",

    // Version file: use version.txt (plain text, just the version string)
    releaseIOMonorepoVersionFile := ((ref: ProjectRef, state: State) =>
      Project.extract(state).get(ref / baseDirectory) / "version.txt"
    ),

    // Read version: plain text
    releaseIOMonorepoReadVersion := ((file: File) => IO.blocking(sbt.IO.read(file).trim)),

    // Write version: plain text
    releaseIOMonorepoVersionFileContents := ((_: File, ver: String) => IO.pure(ver + "\n")),

    releaseIOIgnoreUntrackedFiles := true
  )
