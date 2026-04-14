# files-monorepo-demo

A demonstration Scala sbt monorepo that showcases a custom release plugin for compressing and uploading project data files via HTTP. Built on top of [sbt-release-io-monorepo](https://github.com/scalauser12/sbt-release-io), it illustrates how to customize the monorepo release lifecycle with hook/policy settings and a domain-specific resource hook.

## What this project demonstrates

In a typical sbt monorepo, the release process involves bumping versions, tagging, and publishing JVM artifacts (JARs) to a repository like Maven Central or Artifactory. This demo disables the built-in publish and push phases, then adds a custom `afterTag` hook that:

1. Reads a plain-text `data` file from each subproject
2. Compresses it using gzip (via fs2)
3. Uploads the compressed file to an HTTP server (via http4s)

This pattern is useful for projects where the "artifact" isn't a JAR -- for example, configuration bundles, ML model weights, static assets, or data pipelines.

## Project structure

```
files-monorepo-demo/
  build.sbt                          # Root build definition
  project/
    plugins.sbt                      # Plugin dependencies
    FileProjectsPlugin.scala         # Auto-discovers subprojects
    FileReleasePlugin.scala          # Custom release plugin with an afterTag upload hook
    FileServerStub.scala             # In-memory HTTP server stub
  projects/
    project1/
      data                           # Data file to compress and upload
      version.txt                    # Plain-text version (e.g. "0.1.0-SNAPSHOT")
    project2/
      data
      version.txt
    project3/
      data
      version.txt
```

## How it works

### Dynamic project discovery

Rather than hardcoding subprojects in `build.sbt`, the `FileProjectsPlugin` auto-discovers them at build load time. It scans the `projects/` directory for subdirectories that contain both a `version.txt` file and a `data` file, and registers each as an sbt subproject via `extraProjects`.

### Version management

Each subproject tracks its own version in a plain-text `version.txt` file (e.g. `0.1.0-SNAPSHOT`). The build configures custom version readers and writers through the grouped `sbt-release-io-monorepo` versioning settings:

- `releaseIOMonorepoVersioningFile` -- resolves the version file path per project
- `releaseIOMonorepoVersioningReadVersion` -- reads the version string (using `IO.blocking` for safe file I/O)
- `releaseIOMonorepoVersioningFileContents` -- formats the version string for writing

### Hook and policy customization

The build keeps the standard monorepo lifecycle intact and customizes it through the grouped policy settings rather than a raw process override. On `sbt-release-io-monorepo` `0.11.0`, these grouped `.sbt` keys remain the supported configuration surface for this build:

- `releaseIOMonorepoPolicyEnableSnapshotDependenciesCheck := false`
- `releaseIOMonorepoPolicyEnableRunClean := false`
- `releaseIOMonorepoPolicyEnableRunTests := false`
- `releaseIOMonorepoPolicyEnablePublish := false`
- `releaseIOMonorepoPolicyEnablePush := false`

The custom `FileReleasePlugin` contributes a resource-backed `afterTag` hook, so the upload runs after tags are created and before the next development versions are written.

### Release process

With those policies and hooks in place, the effective `releaseFiles` flow is:

| Step | Description |
|------|-------------|
| `initializeVcs` | Initialize git context |
| `checkCleanWorkingDir` | Ensure no uncommitted changes |
| `resolveReleaseOrder` | Determine dependency order between subprojects |
| `detectOrSelectProjects` | Detect changed projects (via git diff) or prompt for selection |
| `inquireVersions` | Ask for and validate release and next development versions |
| `setReleaseVersions` | Write release versions to `version.txt` files |
| `commitReleaseVersions` | Commit the version changes |
| `tagReleases` | Create git tags (e.g. `project1/v0.1.0`) |
| **`after-tag:compress-and-upload`** | **Gzip and upload each project's data file** |
| `setNextVersions` | Write next development versions (e.g. `0.2.0-SNAPSHOT`) |
| `commitNextVersions` | Commit the next version changes |

The custom `afterTag` hook runs per-project. For each project being released, it:

1. Reads the `data` file as an fs2 byte stream
2. Pipes it through gzip compression
3. Sends a `PUT` request to `http://localhost/files/{project}/{version}/data.gz`
4. Logs the server response

### Stubbed HTTP server

The `FileServerStub` provides an in-memory HTTP server (no network I/O) using http4s `Client.fromHttpApp`. It supports:

- `PUT /files/{project}/{version}/{filename}` -- stores the uploaded bytes in a `Ref`
- `GET /files/{project}/{version}/{filename}` -- retrieves stored bytes
- `GET /files` -- lists all stored keys

This makes the demo fully self-contained -- no external services required.

## Running the release

```bash
# Interactive mode (prompts for versions)
sbt releaseFiles

# Non-interactive with explicit per-project versions
sbt "releaseFiles all-changed with-defaults release-version project1=0.1.0 next-version project1=0.2.0-SNAPSHOT release-version project2=0.1.0 next-version project2=0.2.0-SNAPSHOT release-version project3=0.1.0 next-version project3=0.2.0-SNAPSHOT"
```

## Tech stack

- **Scala 2.12.21** (sbt meta-build)
- **sbt 1.12.8**
- **sbt-release-io-monorepo 0.11.0** -- monorepo-aware release plugin with cats-effect IO
- **cats-effect 3** -- effectful programming
- **http4s 0.23.33** -- HTTP client and server DSL
- **fs2 3.12.2** -- streaming I/O and gzip compression
