addSbtPlugin("io.github.scalauser12" % "sbt-release-io-monorepo" % "0.13.0")

// Meta-build deps for FileReleasePlugin.scala and FileServerStub.scala
libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-client" % "0.23.33",
  "org.http4s" %% "http4s-dsl"    % "0.23.33",
  "co.fs2"     %% "fs2-io"        % "3.12.2"
)
