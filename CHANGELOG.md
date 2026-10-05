# Changelog

All notable changes to this project are documented here. The format is based
on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased] — 0.2.0-SNAPSHOT

This fork is not currently published to Maven Central; install from source
with `mvn install`.

### Fixed

- `SharedString.insert()` applied the inserted text twice: once directly to
  the local value and once again via the editor's operation handler. The
  direct mutation was removed; regression tests added.

### Added

- `otter-examples` module with a console demo (`OtConsoleDemo`) that
  replays three concurrent-editing scenarios (insert/insert at different
  positions, insert/insert at the same position, insert vs delete) and prints
  each transformation step. Exits non-zero if clients diverge.
- `DeltaPrinter` for human-readable rendering of string operations.
- Regression tests for `SharedString.insert()` and `remove()`, plus a
  concurrent-insert convergence test.
- `CONTRIBUTING.md`, CI badge and build/demo instructions in `README.md`.

### Changed

- **Java baseline is now 21** (was 9).
- Build plugins: maven-compiler-plugin 3.14.1, maven-surefire-plugin 3.5.4,
  spotbugs-maven-plugin 4.10.4.1.
- Dependencies: junit 4.13.2 (was 4.9), slf4j 2.0.17 (was 1.7.30, unified
  via the `slf4j.version` property).
- SpotBugs findings triaged to zero: comparator singletons are now `final`,
  switches have `default` branches (unknown string operation type now throws
  `IOException` during deserialization), and intentional patterns are
  suppressed with justifications in `spotbugs-exclude.xml`.
- CI rewritten: GitHub Actions v4 with a Temurin JDK 21/25 matrix (was
  deprecated v1 actions on JDK 9).
- revapi upgraded to 0.15.1 (revapi-java 0.28.4) and moved to the `release`
  profile — API checks gate releases, not day-to-day builds.

### Removed

- Unused `randomizedtesting-runner` test dependency.
- `my-ot/src/java/com/ot/visual/Opt.java` (never compiled, superseded by the
  console demo).
