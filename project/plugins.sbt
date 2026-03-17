addSbtPlugin("io.github.scalauser12" % "sbt-release-io-monorepo" % "0.5.1")

// Meta-build deps for FileReleasePlugin.scala and FileServerStub.scala
libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-client" % "0.23.30",
  "org.http4s" %% "http4s-dsl"    % "0.23.30",
  "co.fs2"     %% "fs2-io"        % "3.12.0"
)
