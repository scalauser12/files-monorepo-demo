lazy val project1 = (project in file("projects/project1"))
  .settings(name := "project1")

lazy val project2 = (project in file("projects/project2"))
  .settings(name := "project2")

lazy val project3 = (project in file("projects/project3"))
  .settings(name := "project3")

lazy val root = (project in file("."))
  .aggregate(project1, project2, project3)
  .enablePlugins(FileReleasePlugin)
  .settings(
    name := "files-monorepo-demo",

    // Version file: use version.txt (plain text, just the version string)
    releaseIOMonorepoVersionFile := { (ref: ProjectRef, state: State) =>
      Project.extract(state).get(ref / baseDirectory) / "version.txt"
    },

    // Read version: plain text
    releaseIOMonorepoReadVersion := { (file: File) =>
      cats.effect.IO.blocking(sbt.IO.read(file).trim)
    },

    // Write version: plain text
    releaseIOMonorepoVersionFileContents := { (_: File, ver: String) =>
      cats.effect.IO.pure(ver + "\n")
    },

    releaseIOIgnoreUntrackedFiles := true
  )
