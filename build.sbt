import cats.effect.IO

lazy val root = (project in file("."))
  .aggregate(FileProjectsPlugin.discoveredProjects.map(p => LocalProject(p.id)): _*)
  .enablePlugins(FileReleasePlugin)
  .settings(
    name         := "files-monorepo-demo",
    scalaVersion := "2.12.21",

    // Version file: use version.txt (plain text, just the version string)
    releaseIOMonorepoVersioningFile := ((ref: ProjectRef, state: State) =>
      Project.extract(state).get(ref / baseDirectory) / FileProjectsPlugin.versionFileName
    ),

    // Read version: plain text
    releaseIOMonorepoVersioningReadVersion := ((file: File) => IO.blocking(sbt.IO.read(file).trim)),

    // Write version: plain text
    releaseIOMonorepoVersioningFileContents := ((_: File, ver: String) => IO.pure(ver + "\n")),

    // Preserve the demo's upload-only release flow while using the hook API.
    releaseIOMonorepoPolicyEnableSnapshotDependenciesCheck := false,
    releaseIOMonorepoPolicyEnableRunClean                  := false,
    releaseIOMonorepoPolicyEnableRunTests                  := false,
    releaseIOMonorepoPolicyEnablePublish                   := false,
    releaseIOMonorepoPolicyEnablePush                      := false,

    // Detect changed projects via git diff (default: true)
    releaseIOMonorepoDetectionEnabled := true,

    // Shared core VCS setting reused by the monorepo clean-working-dir check.
    releaseIOVcsIgnoreUntrackedFiles := true
  )
