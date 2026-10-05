# Contributing

Thanks for your interest in contributing to Otter!

## Setup

- Java 21+ and Maven 3.6+
- Build and test: `mvn install`

## Code style

- This project uses [EditorConfig](.editorconfig) — Java sources are indented
  with tabs. Please configure your editor to respect `.editorconfig`.
- Keep the existing style: opening braces on their own line, no wildcard
  imports.

## Tests

- Every behavior change needs a test. Run the suite with `mvn test`.
- Regression tests for bug fixes should fail when the fix is reverted.

## Static analysis

- SpotBugs runs during `mvn install`/`mvn verify`. Intentional patterns are
  suppressed in [spotbugs-exclude.xml](spotbugs-exclude.xml) — add new
  suppressions there with a justification comment.
- API compatibility (revapi) is checked in the `release` profile:
  `mvn -Prelease verify -Dgpg.skip=true`.

## Commits

- Use conventional commits: `feat:`, `fix:`, `test:`, `build:`, `docs:`,
  `chore:`.
- Keep commits small and focused; one logical change per commit.

## Pull requests

- CI must be green on all JDK versions in the matrix.
- Describe the motivation for the change, not just the diff.
