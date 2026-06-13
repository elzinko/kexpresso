# Contributing to Kexpresso ☕

Thank you for helping improve Kexpresso! This guide covers everything you need to go from
zero to a merged pull request.

New to the codebase? Start with [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — a short
map of the files, the AST at the core of the design, and where to start reading.

---

## Prerequisites

| Tool | Required version |
|---|---|
| JDK | 17 or later |
| Git | any recent version |

No other global installs are required — the Gradle wrapper (`./gradlew`) downloads all
build tooling automatically on first run.

---

## Building locally

```bash
# Full build: compile + tests + Detekt static analysis + Kover coverage check
./gradlew build

# Tests only
./gradlew test

# Detekt static analysis only
./gradlew detekt

# Kover coverage report (written to build/reports/kover/)
./gradlew koverHtmlReport
```

All three checks — tests, Detekt, and coverage — must pass before a pull request will be
merged.

---

## Code style

- **Kotlin official style** — enforced via `kotlin.code.style=official` in
  `gradle.properties`.
- **Detekt** — configuration lives at `config/detekt/detekt.yml`. Run `./gradlew detekt`
  locally before pushing; CI will fail on any new violation.

Key points:
- 4-space indentation, no tabs.
- Keep functions small and focused.
- Every public API member needs a KDoc comment (see existing methods in
  `src/commonMain/kotlin/kexpresso/` for style examples).

---

## Branching and pull-request conventions

| Convention | Detail |
|---|---|
| Base branch | `main` |
| Feature branches | `feat/<short-description>` |
| Bug-fix branches | `fix/<short-description>` |
| Docs branches | `docs/<short-description>` |
| Commit style | [Conventional Commits](https://www.conventionalcommits.org/) |

### Conventional Commits

```
feat: add lookahead DSL primitive
fix: escape hyphen inside noneOf character class
docs: expand quickstart examples in README
test: add edge-case tests for between() with greedy=false
refactor: extract renderBlock helper into a sealed class
chore: bump Kotlin to 1.9.0
```

### CI requirements

Every PR must have:
- Green CI build (tests + Detekt + Kover).
- Code coverage maintained or improved (do not lower the threshold).
- Tests for every new or changed DSL method.
- Updated KDoc on every public API addition.
- An entry in the `[Unreleased]` section of `CHANGELOG.md`.

---

## Adding a new DSL primitive (TDD workflow)

Follow these steps when adding a method to `KexpressoBuilder` (or a new extension file).
A primitive touches more files than you might expect (codegen and example-generation
lookup tables) — see the
[touchpoint checklist in ARCHITECTURE.md](docs/ARCHITECTURE.md#adding-a-new-primitive)
for the complete list.

### 1. Write a failing test

Add a test class or test function in the appropriate file under
`src/commonTest/kotlin/kexpresso/` (portable behaviour) or
`src/jvmTest/kotlin/kexpresso/` (JVM-specific). Name the test descriptively:

```kotlin
@Test
fun `lookahead requires the following chars to match`() {
    val p = kexpresso { literal("Espresso"); lookahead { literal("!") } }
    assertTrue(p.containsMatchIn("Espresso!"))
    assertFalse(p.containsMatchIn("Espresso."))
}
```

Run `./gradlew test` — the test must fail at this point.

### 2. Implement on `KexpressoBuilder` (or as an extension)

- **Core primitive** — add the method to `KexpressoBuilder` in
  `src/commonMain/kotlin/kexpresso/Kexpresso.kt`, backed by an AST node from `Ast.kt`
  (see `followedBy` and the other lookarounds for the pattern to follow).
- **Domain helper** — add a Kotlin extension function in the relevant file
  (`Text.kt`, `Writing.kt`, `Domains.kt`) or create a new file following the same
  naming pattern; helpers emit their regex via the internal `append(...)` shim.

### 3. Write the KDoc

Every public method must have a KDoc comment that includes:
- A one-line summary.
- `@param` tags for each parameter.
- At least one example showing the regex produced (e.g. `// produces (?=...)`) or a
  usage snippet.

### 4. Verify the full build is green

```bash
./gradlew build
```

All tests must pass, Detekt must report no new violations, and coverage must not drop.

### 5. Update `CHANGELOG.md`

Add a short line under `## [Unreleased]` describing what you added.

### 6. Open a pull request

Use the [PR template](.github/PULL_REQUEST_TEMPLATE.md). Fill in the summary and
checklist, then request a review.

---

## Reporting bugs and requesting features

Use the GitHub issue templates:

- [Bug report](.github/ISSUE_TEMPLATE/bug_report.md)
- [Feature request](.github/ISSUE_TEMPLATE/feature_request.md)

---

## License

By contributing you agree that your changes will be released under the [MIT License](LICENSE).
