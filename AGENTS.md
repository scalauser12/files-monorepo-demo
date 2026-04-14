# files-monorepo-demo

A demonstration Scala sbt monorepo showcasing a custom release plugin that compresses and uploads project data files via HTTP. Uses `sbt-release-io-monorepo` `0.11.0` with a custom `FileReleasePlugin`.

Three subprojects:
- **project1** (`projects/project1/`) — version tracked in `version.txt`
- **project2** (`projects/project2/`) — version tracked in `version.txt`
- **project3** (`projects/project3/`) — version tracked in `version.txt`

Scala 2.12 (sbt meta-build). sbt 1.12.8. cats-effect 3. http4s 0.23. fs2 3.12.

## Build & Test Commands

- `sbt compile` — compile the build definition (meta-build)
- `sbt releaseFiles with-defaults` — run the full release process (compress, upload, tag, version bump)
- `sbt releaseFiles with-defaults release-version 0.1.0 next-version 0.2.0-SNAPSHOT` — release with explicit versions

## Key Files

- `build.sbt` — root project config, aggregates project1/2/3, configures FileReleasePlugin
- `project/FileReleasePlugin.scala` — custom release plugin: defines `compressAndUploadHook`, contributes an `afterTag` resource hook
- `project/FileServerStub.scala` — in-memory HTTP server stub simulating file storage
- `project/plugins.sbt` — plugin dependencies (sbt-release-io-monorepo, http4s, fs2)
- `projects/project{1,2,3}/version.txt` — plain-text version files (read/written by custom IO handlers)
- `projects/project{1,2,3}/data` — data files compressed and uploaded during release

## Coding Conventions

- Scala 2.12 with `-Xsource:3` — `import foo.{*, given}` and `[?]` wildcards are valid
- Use cats-effect `IO` for all effectful operations; wrap blocking calls in `IO.blocking`
- Error handling: use `IO.raiseError` instead of throwing, `scala.util.control.NonFatal` in catch blocks
- Prefer `_root_.io.release.X` when `import sbt.*` shadows the `io` package
- Use `for`-comprehensions for sequential IO composition
- http4s: use `Client[IO]` for HTTP requests, DSL for route definitions
- fs2: use `Stream[IO, Byte]` for file streaming

## Compaction Instructions

When compacting this conversation, preserve the following:

1. **What the user asked for** — the original request and any clarifications, verbatim or close to it
2. **Which files were modified** — full paths and a one-line summary of each change
3. **Which files were read but not modified** — so they don't need to be re-read
4. **Current task state** — what's done, what's in progress, what's pending
5. **Errors encountered and how they were resolved** — especially:
   - `import sbt.*` shadowing `io.release` → use `_root_.io.release`
6. **The active plan file path** if plan mode was used
7. **Key architectural decisions made**

Do NOT preserve:
- Full file contents that were merely read for context
- Redundant intermediate states of files that were edited multiple times
- Tool call details / raw output beyond what's needed to understand the change
