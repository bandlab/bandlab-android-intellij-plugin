# Contributing

This document covers the local setup, day-to-day development loop, testing expectations, changelog policy, and code
style.

## Project layout

- `src/main/kotlin/com/bandlab/intellij/plugin/` — plugin sources, organized by feature package.
- `src/main/resources/META-INF/plugin.xml` — plugin descriptor (actions, extensions). The description and change notes
  are injected at build time from
  `README.md` and `CHANGELOG.md`.
- `src/test/kotlin/...` — tests, mirroring the main package layout.
- `.run/` — shared IntelliJ run configurations (checked into VCS).

## Development loop

| Task                            | Command                  | Run config          |
|---------------------------------|--------------------------|---------------------|
| Run the plugin in a sandbox IDE | `./gradlew runIde`       | `Run Plugin`        |
| Run tests / full check          | `./gradlew check`        | `Run Tests`         |
| Verify plugin structure         | `./gradlew verifyPlugin` | `Run Verifications` |
| Build a distributable zip       | `./gradlew buildPlugin`  | —                   |

`./gradlew check` runs the test suite and produces a Kover coverage report at `build/reports/kover/report.xml`.
`verifyPlugin` runs the IntelliJ Plugin Verifier and must pass before merging.

CI (`.github/workflows/build.yml`) runs `buildPlugin`, `check`, and `verifyPlugin` on every pull request.

## Changelog (required for every PR)

Every user-facing change must be recorded in the `## Unreleased` section of [`CHANGELOG.md`](/CHANGELOG.md) **within the
same PR**. The file follows the [Keep a Changelog][keepachangelog] format. Add your entry under the appropriate group
heading:

- `### Added` — new features.
- `### Changed` — changes to existing behavior.
- `### Fixed` — bug fixes.

```markdown
## Unreleased

### Added

- Short, user-facing description of what you added
```

The changelog drives the plugin's marketplace change notes, so write entries from the user's perspective. Purely
internal changes (refactors, test-only changes, CI tweaks) don't need an entry.

## Testing (required for new work)

New code must be covered by tests. Choose the right style:

- **New features → integration tests** using [`BasePlatformTestCase`][plugin-testing]
  (`com.intellij.testFramework.fixtures.BasePlatformTestCase`). These exercise actions, annotators, completion, wizards,
  etc. against a real IntelliJ platform fixture. See existing examples such as `template/CreateTemplateActionTest.kt`,
  `module/BandLabModuleWizardStepIntegrationTest.kt`, and
  `dependencies/autocomplete/GradleProjectAnnotatorIntegrationTest.kt`.
- **Pure business logic → plain unit tests** (JUnit + Truth, no platform fixture). See examples such as
  `jenkins/TargetsTest.kt`, `localizer/GitBranchTest.kt`, and `utils/BuildScriptUtilsTest.kt`.

Test conventions:

- Frameworks: **JUnit 4** (`@Test`), **Google Truth** (`assertThat`), plus Turbine / kotlinx-coroutines-test for Flow
  and coroutine code.
- Place tests under `src/test/kotlin`, mirroring the package of the code under test.
- Run everything with `./gradlew check`.

## Code style

This repository uses [Kempt][kempt] to enforce code style. Kempt runs [ktfmt][ktfmt] (`kotlinlang` style) on Kotlin
sources, sorts Gradle dependency blocks, normalizes trailing whitespace, and inserts Apache 2.0 license headers.
Configuration lives in [`.kempt.toml`](/.kempt.toml).

CI runs `kempt check` on every push and pull request. A pull request will fail if any file is not formatted.

### Install Kempt

A working Git 2.25+ and a JDK 17+ on your `PATH` are required.

```bash
# Homebrew (macOS, Linux)
brew install ZacSweers/tap/kempt-fmt

# or the shell installer
curl --proto '=https' --tlsv1.2 -LsSf \
  https://github.com/ZacSweers/kempt/releases/latest/download/kempt-fmt-installer.sh | sh

# or via Cargo
cargo install kempt-fmt
```

### Set up the commit hook locally

Install the pre-commit hook so your staged files are formatted automatically before each commit:

```bash
kempt install-hook
```

This writes `.git/hooks/pre-commit`, which calls `kempt hook`. In the default `format` mode the hook formats matching
staged files and re-stages only the files it changed before the commit continues.

Because `.git/hooks/` is local Git metadata, every contributor must run `kempt install-hook` once after cloning.

### Formatting manually

```bash
kempt format   # format all tracked files in place
kempt format --staged   # format only staged files
kempt check   # read-only; exits non-zero if anything needs formatting (what CI runs)
```

[//]: # (Links)

[keepachangelog]: https://keepachangelog.com
[plugin-testing]: https://plugins.jetbrains.com/docs/intellij/testing-plugins.html
[kempt]: https://github.com/ZacSweers/kempt
[ktfmt]: https://github.com/Kotlin/ktfmt
