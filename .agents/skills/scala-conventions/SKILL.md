---
name: scala-conventions
description: Scala/sbt coding conventions for files-monorepo-demo. Apply when writing or reviewing code.
user-invocable: false
---

## Language & Build

- Scala 2.12 with `scala212source3` dialect
- sbt 1.12.6
- cats-effect 3, http4s 0.23, fs2 3.12

## Import Style

- **Grouping order**: standard library → third-party (cats-effect, http4s, fs2) → project-internal (`io.release.*`) → sbt framework (`sbt.*`) → scala language imports
- Use wildcard `*` syntax (not `_`): `import sbt.*`, `import sbt.Keys.*`
- Use `as` for aliasing (never `=>`): `import foo.{Bar as B, *}`
- Use `[?]` for wildcard types (not `[_]`): `Seq[Setting[?]]`
- **Import shadowing**: Use `_root_.io.release.X` when `import sbt.*` shadows the `io` package

## Naming

- Classes/Traits/Objects: PascalCase (`FileReleasePlugin`, `FileServerStub`)
- Methods/vals: camelCase (`compressAndUploadStep`, `releaseFiles`)
- Type parameters: single uppercase letter (`[T]`, `[A]`)
- Setting keys: camelCase (`releaseFilesProcess`)

## cats-effect IO Patterns

- `IO.blocking { ... }` for all blocking operations (sbt task execution, file I/O, VCS)
- `IO.pure(value)` for already-computed values
- `IO { ... }` for lightweight side effects
- `IO.raiseError(new RuntimeException("message"))` for errors
- `for`-comprehensions for sequential IO composition
- `*>` for sequencing and discarding left result
- `.as(value)` for replacing result
- `.void` for discarding result
- `IO.defer` for lazy evaluation
- `.handleErrorWith` for error recovery
- `unsafeRunSync()` only at plugin entry-point boundaries

## http4s Patterns

- Use `Client[IO]` for HTTP requests
- Use `Resource[IO, Client[IO]]` for client lifecycle
- DSL for route definitions: `HttpRoutes.of[IO] { case req @ PUT -> Root / "path" => ... }`
- Use `Status.Ok`, `Status.NotFound` etc. for response matching

## fs2 Patterns

- `Stream[IO, Byte]` for file streaming
- `Files[IO].readAll(path)` for reading files as streams
- `.through(Compression[IO].gzip())` for compression
- `.compile.to(Array)` or `.compile.drain` for materializing streams

## Error Handling

- Use `IO.raiseError` instead of throwing exceptions
- Pattern match with `handleErrorWith` for recovery
- Never use try-catch in IO code
- Catch blocks must use `scala.util.control.NonFatal`, never bare catch-all

## Data & Immutability

- All case classes immutable; use `copy()` for modifications
- No `var` in production code
- No `null` values
- Use `lazy val` for expensive initialization

## Functional Patterns

- `foldLeft` with `IO.pure` seed for composing step sequences
- Higher-order functions for step composition
- Pattern matching over `Option` and `List` (avoid `.get`)

## sbt Plugin Patterns

- `AutoPlugin` with `override def trigger = allRequirements`
- Export keys via `object autoImport { ... }`
- Use `lazy val projectSettings: Seq[Setting[?]]`
- State threading: `Project.extract(state).runTask(key, state)`

## Logging

- Always prefix with `[release-io]`: `state.log.info("[release-io] message")`
- Use string interpolation: `s"[release-io] Variable: $value"`
