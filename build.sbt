import cats.effect.IO

lazy val root = (project in file("."))
  .aggregate(FileProjectsPlugin.discoveredProjects.map(p => LocalProject(p.id)): _*)
  .enablePlugins(FileReleasePlugin)
  .settings(
    name         := "files-monorepo-demo",
    scalaVersion := "2.12.21",

    // Version file: use version.txt (plain text, just the version string)
    releaseIOMonorepoVersionFile := ((ref: ProjectRef, state: State) =>
      Project.extract(state).get(ref / baseDirectory) / FileProjectsPlugin.versionFileName
    ),

    // Read version: plain text
    releaseIOMonorepoReadVersion := ((file: File) => IO.blocking(sbt.IO.read(file).trim)),

    // Write version: plain text
    releaseIOMonorepoVersionFileContents := ((_: File, ver: String) => IO.pure(ver + "\n")),

    // Preserve the demo's upload-only release flow while using the hook API.
    releaseIOMonorepoEnableSnapshotDependenciesCheck := false,
    releaseIOMonorepoEnableRunClean                  := false,
    releaseIOMonorepoEnableRunTests                  := false,
    releaseIOMonorepoEnablePublish                   := false,
    releaseIOMonorepoEnablePush                      := false,

    // Detect changed projects via git diff (default: true)
    releaseIOMonorepoDetectChanges := true,

    // Shared core VCS setting reused by the monorepo clean-working-dir check.
    releaseIOIgnoreUntrackedFiles := true
  )
